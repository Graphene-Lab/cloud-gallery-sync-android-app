package com.cloud.sync.background

import android.content.Context
import android.content.ContentResolver
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.cloud.communication.cryto.IZeroKnowledgeProof
import com.cloud.sync.common.config.SyncConfig
import com.cloud.sync.domain.model.GalleryPhoto
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import com.cloud.sync.manager.interfaces.ICloudManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.IOException
import java.io.ByteArrayInputStream
import android.net.Uri

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("PhotoSyncWorker Unit Tests")
class PhotoSyncWorkerTest {

    //region Mocks and Test Doubles
    /** Mock for the repository managing sync intervals and pointers. */
    private lateinit var syncRepository: ISyncRepository
    /** Mock for the repository that accesses the device's media gallery. */
    private lateinit var galleryRepository: IGalleryRepository
    /** Mock for application-wide synchronization settings. */
    private lateinit var syncConfig: SyncConfig
    /** Mock for accessing file content from the device. */
    private lateinit var contentResolver: ContentResolver
    /** Mock for zero-knowledge proof encryption. */
    private lateinit var zeroKnowledgeProof: IZeroKnowledgeProof
    /** Mock for uploading files to the cloud. */
    private lateinit var dataCenterCloudManager: ICloudManager
    /** Mocks for the Android framework dependencies required by the Worker's constructor. */
    private lateinit var context: Context
    private lateinit var workerParameters: WorkerParameters
    //endregion

    /** The instance of the CoroutineWorker that we are testing. */
    private lateinit var worker: PhotoSyncWorker

    @BeforeEach
    fun setup() {
        // --- Initialize Mocks ---
        syncRepository = mock()
        galleryRepository = mock()
        syncConfig = mock()
        contentResolver = mock()
        zeroKnowledgeProof = mock()
        dataCenterCloudManager = mock()
        context = mock()
        workerParameters = mock()

        // --- Set Up Common Mock Behavior ---
        // Define a consistent batch size for all tests to use.
        whenever(syncConfig.batchSize).thenReturn(3)
        // Set photoFolderPath to default value
        whenever(syncConfig.photoFolderPath).thenReturn("Photos/")
        // Set encryption disabled by default for tests
        whenever(syncConfig.isEncryptionEnabled).thenReturn(false)

        // --- Instantiate the Worker ---
        // We create the worker directly, injecting our mocks. This allows us to test its logic
        // without involving the actual WorkManager framework.
        worker = PhotoSyncWorker(
            context,
            workerParameters,
            syncRepository,
            galleryRepository,
            syncConfig,
            contentResolver,
            zeroKnowledgeProof,
            dataCenterCloudManager
        )
    }

    @Nested
    @DisplayName("Worker Execution Scenarios")
    inner class WorkerExecutionTests {

        @Test
        @DisplayName("should return success and do nothing if 'Sync From Now' point is not set")
        fun doWork_whenSyncPointIsNotSet_returnsSuccessAndDoesNothing() = runTest {
            // Arrange
            // Simulate the case where the user has not enabled this feature.
            whenever(syncRepository.syncFromNowPoint).thenReturn(MutableStateFlow(0L))

            // Act
            val result = worker.doWork()

            // Assert
            assertEquals(Result.success(), result)
            // Verify that no further repository interactions occurred.
            verify(galleryRepository, never()).getPhotos(any())
            verify(syncRepository, never()).saveSyncedIntervals(any())
        }

        @Test
        @DisplayName("should return failure if the anchor interval for 'Sync From Now' is missing")
        fun doWork_whenAnchorIntervalNotFound_returnsFailure() = runTest {
            // Arrange
            // Simulate that a sync point exists...
            whenever(syncRepository.syncFromNowPoint).thenReturn(MutableStateFlow(1000L))
            // ...but the corresponding interval is missing from the database (a data integrity issue).
            val intervals = listOf(TimeInterval(start = 1L, end = 100L))
            whenever(syncRepository.syncedIntervals).thenReturn(MutableStateFlow(intervals))

            // Act
            val result = worker.doWork()

            // Assert
            // The worker should fail fast to indicate a problem that requires intervention.
            assertEquals(Result.failure(), result)
        }

        @Test
        @DisplayName("should return success if no new photos are found")
        fun doWork_whenNoNewPhotos_returnsSuccess() = runTest {
            // Arrange
            val anchorInterval = TimeInterval(start = 1000L, end = 2000L)
            whenever(syncRepository.syncFromNowPoint).thenReturn(MutableStateFlow(1000L))
            whenever(syncRepository.syncedIntervals).thenReturn(MutableStateFlow(listOf(anchorInterval)))
            // Simulate the gallery returning no new photos since the last sync.
            whenever(galleryRepository.getPhotos(startTimeSeconds = anchorInterval.end + 1)).thenReturn(emptyList())

            // Act
            val result = worker.doWork()

            // Assert
            assertEquals(Result.success(), result)
            // Verify no attempt was made to save new intervals.
            verify(syncRepository, never()).saveSyncedIntervals(any())
        }

        @Test @DisplayName("should sync new photos in batches and return success")
        fun doWork_withNewPhotos_syncsInBatchesAndReturnsSuccess() = runTest {
            // Arrange
            val anchorInterval = TimeInterval(start = 1000L, end = 2000L)
            // IMPORTANT: Create a fresh mutable list for the worker to modify.
            val initialIntervals = mutableListOf(anchorInterval)

            whenever(syncRepository.syncFromNowPoint).thenReturn(MutableStateFlow(1000L))
            whenever(syncRepository.syncedIntervals).thenReturn(MutableStateFlow(initialIntervals))

            val newPhotos = listOf(
                GalleryPhoto(id = 1, dateAdded = 2001, displayName = "a.jpg", path = mock<Uri>()),
                GalleryPhoto(id = 2, dateAdded = 2002, displayName = "b.jpg", path = mock<Uri>()),
                GalleryPhoto(id = 3, dateAdded = 2003, displayName = "c.jpg", path = mock<Uri>()),
                GalleryPhoto(id = 4, dateAdded = 2004, displayName = "d.jpg", path = mock<Uri>())
            )
            whenever(galleryRepository.getPhotos(startTimeSeconds = anchorInterval.end + 1)).thenReturn(newPhotos)

            // Mock ContentResolver to return input streams for file reads
            // Use doAnswer to create a fresh ByteArrayInputStream for each call
            doAnswer { invocation ->
                ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
            }.`when`(contentResolver).openInputStream(any())

            // Mock dataCenterCloudManager.uploadFileBytes to accept any payload and progress callback
            doAnswer { invocation ->
                val progressCallback = invocation.getArgument<(com.cloud.communication.cryto.FileUploader.ChunkProgress) -> Unit>(3)
                // Simulate completion
                progressCallback.invoke(
                    com.cloud.communication.cryto.FileUploader.ChunkProgress(
                        "test.jpg",
                        1,
                        1,
                        true
                    )
                )
                null
            }.`when`(dataCenterCloudManager).uploadFileBytes(any(), any(), any(), any())

            // --- THE FIX: Capture Snapshots, Not References ---
            // Create a list to hold deep copies of the arguments at the time of invocation.
            val capturedIntervalLists = mutableListOf<List<TimeInterval>>()

            // Use doAnswer to manually capture a snapshot of the argument.
            // The .toList() call creates a new, immutable copy.
            doAnswer { invocation ->
                val listArgument = invocation.getArgument<List<TimeInterval>>(0)
                capturedIntervalLists.add(listArgument.toList()) // Create and add a copy
                null // Return null because the method has a Unit/void return type
            }.`when`(syncRepository).saveSyncedIntervals(any())


            // Act
            val result = worker.doWork()
            advanceUntilIdle()

            // Assert
            assertEquals(Result.success(), result)

            // Now, we assert against our manually captured list of snapshots.
            assertEquals(2, capturedIntervalLists.size, "saveSyncedIntervals should have been called twice.")

            // Assert the state after the first batch was saved.
            val firstSave = capturedIntervalLists[0]
            assertEquals(1, firstSave.size)
            assertEquals(1000L, firstSave[0].start)
            assertEquals(2003L, firstSave[0].end, "End timestamp should be updated to the last photo of the first batch.")

            // Assert the state after the final batch was saved.
            val secondSave = capturedIntervalLists[1]
            assertEquals(1, secondSave.size)
            assertEquals(1000L, secondSave[0].start)
            assertEquals(2004L, secondSave[0].end, "End timestamp should be updated to the last photo of the final batch.")
        }

        @Test
        @DisplayName("should return retry when a repository throws an exception")
        fun doWork_whenRepositoryThrowsException_returnsRetry() = runTest {
            // Arrange
            val anchorInterval = TimeInterval(start = 1000L, end = 2000L)
            whenever(syncRepository.syncFromNowPoint).thenReturn(MutableStateFlow(1000L))
            whenever(syncRepository.syncedIntervals).thenReturn(MutableStateFlow(listOf(anchorInterval)))

            // THE FIX: Use doAnswer to throw a checked exception from a suspend function.
            val exception = IOException("Failed to read from storage")
            doAnswer { throw exception }
                .`when`(galleryRepository).getPhotos(any())

            // Act
            val result = worker.doWork()
            advanceUntilIdle()

            // Assert
            // The worker should request a retry for transient errors like I/O failures.
            assertEquals(Result.retry(), result)
        }
    }
}
