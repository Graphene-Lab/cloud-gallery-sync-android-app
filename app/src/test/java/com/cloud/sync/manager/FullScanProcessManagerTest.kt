
package com.cloud.sync.manager

import android.content.ContentResolver
import android.net.Uri
import com.cloud.communication.cryto.FileUploader
import com.cloud.communication.cryto.IZeroKnowledgeProof
import com.cloud.sync.common.config.SyncConfig
import com.cloud.sync.domain.model.GalleryPhoto
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import com.cloud.sync.manager.interfaces.ICloudManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("FullScanProcessManager Unit Tests")
class FullScanProcessManagerTest {

    // Mockito mocks for dependencies of FullScanProcessManager
    private lateinit var syncIntervalRepository: ISyncRepository
    private lateinit var galleryRepository: IGalleryRepository
    private lateinit var syncConfig: SyncConfig
    private lateinit var contentResolver: ContentResolver
    private lateinit var zeroKnowledgeProof: IZeroKnowledgeProof
    private lateinit var dataCenterCloudManager: ICloudManager

    // The class under test
    private lateinit var fullScanProcessManager: FullScanProcessManager

    // Coroutine test dispatcher and scheduler for controlling and advancing virtual time
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeEach
    fun setup() {
        // Initialize mocks using Mockito before each test to ensure a clean state
        syncIntervalRepository = mock()
        galleryRepository = mock()
        syncConfig = mock()
        contentResolver = mock()
        zeroKnowledgeProof = mock()
        dataCenterCloudManager = mock()

        // Configure common mock behavior: set the batch size for testing
        whenever(syncConfig.batchSize).thenReturn(3)

        // Mock ContentResolver behavior
        whenever(contentResolver.openInputStream(any<Uri>())).thenReturn(ByteArrayInputStream("dummy_photo_data".toByteArray()))

        // Mock IZeroKnowledgeProof behavior
        whenever(zeroKnowledgeProof.EncryptFullFileName(any())).thenAnswer { args ->
            "encrypted_" + args.arguments[0] as String
        }
        whenever(zeroKnowledgeProof.encryptBytes(any(), any(), any())).thenAnswer { args ->
            val originalBytes = args.arguments[0] as ByteArray
            // Simulate encryption by simply transforming bytes, e.g., reversing
            originalBytes.reversedArray()
        }

        // Mock ICloudManager behaviour using Mockito's doNothing
        doNothing().whenever(dataCenterCloudManager).uploadFileBytes(
            any(),
            any(),
            any(),
            any<(FileUploader.ChunkProgress) -> Unit>()
        )

        // Create the instance of the class under test with its mocked dependencies
        fullScanProcessManager = FullScanProcessManager(
            syncIntervalRepository,
            galleryRepository,
            syncConfig,
            contentResolver,
            zeroKnowledgeProof,
            dataCenterCloudManager
        )

        Dispatchers.setMain(testDispatcher)

    }
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @Nested
    @DisplayName("initializeIntervals Tests")
    inner class InitializeIntervalsTests {

        @Test
        @DisplayName("should add (0,0) interval if repository is empty")
        fun initializeIntervals_emptyRepository_addsZeroInterval() = runTest(testDispatcher) {
            // Arrange: Mock the repository to return an empty list of intervals
            whenever(syncIntervalRepository.syncedIntervals).thenReturn(MutableStateFlow(emptyList()))

            // Act: Call the method under test
            val result = fullScanProcessManager.initializeIntervals()

            // Assert: Verify the expected outcome
            assertEquals(1, result.size)
            assertEquals(TimeInterval(0, 0), result[0])
        }

        @Test
        @DisplayName("should add (0,0) interval if repository has other intervals but not (0,0)")
        fun initializeIntervals_repositoryWithoutZeroInterval_addsZeroInterval() =
            runTest(testDispatcher) {
                // Arrange: Mock the repository with existing intervals, but no (0,0)
                val existingIntervals =
                    mutableListOf(TimeInterval(100, 200), TimeInterval(300, 400))
                whenever(syncIntervalRepository.syncedIntervals).thenReturn(
                    MutableStateFlow(
                        existingIntervals
                    )
                )

                // Act
                val result = fullScanProcessManager.initializeIntervals()

                // Assert: Verify (0,0) was added at the beginning and other intervals are present
                assertEquals(3, result.size)
                assertEquals(TimeInterval(0, 0), result[0])
                assertEquals(TimeInterval(100, 200), result[1])
                assertEquals(TimeInterval(300, 400), result[2])
            }

        @Test
        @DisplayName("should not add (0,0) interval if it already exists")
        fun initializeIntervals_repositoryWithZeroInterval_doesNotAddDuplicate() =
            runTest(testDispatcher) {
                // Arrange: Mock the repository with (0,0) already existing
                val existingIntervals = mutableListOf(TimeInterval(0, 50), TimeInterval(100, 200))
                whenever(syncIntervalRepository.syncedIntervals).thenReturn(
                    MutableStateFlow(
                        existingIntervals
                    )
                )

                // Act
                val result = fullScanProcessManager.initializeIntervals()

                // Assert: Verify no duplicate (0,0) was added
                assertEquals(2, result.size)
                assertEquals(TimeInterval(0, 50), result[0])
                assertEquals(TimeInterval(100, 200), result[1])
            }

        @Test
        @DisplayName("should sort intervals by start timestamp")
        fun initializeIntervals_unsortedIntervals_sortsThem() = runTest(testDispatcher) {
            // Arrange: Mock repository with unsorted intervals, including 0
            val unsortedIntervals =
                mutableListOf(TimeInterval(100, 200), TimeInterval(0, 50), TimeInterval(300, 400))
            whenever(syncIntervalRepository.syncedIntervals).thenReturn(
                MutableStateFlow(
                    unsortedIntervals
                )
            )

            // Act
            val result = fullScanProcessManager.initializeIntervals()

            // Assert: Verify the intervals are sorted by their start timestamp
            assertEquals(TimeInterval(0, 50), result[0])
            assertEquals(TimeInterval(100, 200), result[1])
            assertEquals(TimeInterval(300, 400), result[2])
        }
    }

    @Nested
    @DisplayName("processNextTwoIntervals Tests")
    inner class ProcessNextTwoIntervalsTests {

        @Test
        @DisplayName("should merge two contiguous intervals without photos in gap")
        fun processNextTwoIntervals_contiguousNoGapPhotos_mergesCorrectly() =
            runTest(testDispatcher) {
                // Arrange: Setup intervals that are contiguous and no photos in gap
                val initialIntervals = mutableListOf(
                    TimeInterval(0, 100),
                    TimeInterval(101, 200),
                    TimeInterval(201, 300)
                )
                whenever(
                    galleryRepository.getPhotosInInterval(
                        any(),
                        any()
                    )
                ).thenReturn(emptyList()) // No photos in gap
                whenever(syncIntervalRepository.syncedIntervals).thenReturn(
                    MutableStateFlow(
                        initialIntervals
                    )
                )

                // Act: Process the intervals
                val result = fullScanProcessManager.processNextTwoIntervals(
                    initialIntervals,
                    coroutineContext
                )
                // Advance virtual time to allow any simulated delays (e.g., in syncAndSaveInBatches) to complete
                advanceUntilIdle()

                // Assert: Verify the two initial intervals are merged into one
                assertEquals(2, result.size)
                assertEquals(TimeInterval(0, 200), result[0]) // Merged (0,100) and (101,200)
                assertEquals(TimeInterval(201, 300), result[1])
                // Verify that saveSyncedIntervals was called exactly once with the final merged list
                verify(syncIntervalRepository).saveSyncedIntervals(argThat {
                    this.size == 2 && this[0] == TimeInterval(
                        0,
                        200
                    ) && this[1] == TimeInterval(201, 300)
                })
            }

        @Test
        @DisplayName("should sync photos in gap and then merge intervals")
        fun processNextTwoIntervals_withPhotosInGap_syncsAndMerges() = runTest(testDispatcher) {
            // Arrange: Setup intervals with a gap and mock photos within that gap
            val initialIntervals = mutableListOf(TimeInterval(0, 100), TimeInterval(150, 200))
            val photosInGap = listOf(
                GalleryPhoto(
                    id = 1L,
                    dateAdded = 110L,
                    displayName = "photo1.jpg",
                    path = mock<Uri>()
                ),
                GalleryPhoto(
                    id = 2L,
                    dateAdded = 120L,
                    displayName = "photo2.jpg",
                    path = mock<Uri>()
                ),
                GalleryPhoto(
                    id = 3L,
                    dateAdded = 130L,
                    displayName = "photo3.jpg",
                    path = mock<Uri>()
                ),
                GalleryPhoto(
                    id = 4L,
                    dateAdded = 140L,
                    displayName = "photo4.jpg",
                    path = mock<Uri>()
                )
            )
            whenever(galleryRepository.getPhotosInInterval(101, 149)).thenReturn(photosInGap)
            whenever(syncIntervalRepository.syncedIntervals).thenReturn(
                MutableStateFlow(
                    initialIntervals
                )
            )

            // Act
            val result =
                fullScanProcessManager.processNextTwoIntervals(initialIntervals, coroutineContext)
            advanceUntilIdle() // Ensure all suspend calls, including batch processing, complete

            // Assert: Verify the gap is filled and intervals are merged, resulting in one continuous interval
            assertEquals(1, result.size)
            assertEquals(
                TimeInterval(0, 200),
                result[0]
            ) // Merged (0,100), filled gap up to 140, then merged with (150,200)

            // Verify interactions: galleryRepository was called to get photos
            verify(galleryRepository).getPhotosInInterval(101, 149)

            // Verify that saveSyncedIntervals was called multiple times due to batching
            // Expected calls:
            // 1. After the first batch (3 photos: 110, 120, 130), interval (0, 130) is saved.
            // 2. After the remaining photos (1 photo: 140), interval (0, 140) is saved.
            // 3. Finally, after the merge with (150, 200), the (0, 200) interval is saved.
            verify(syncIntervalRepository, times(3)).saveSyncedIntervals(any())

            // Use argument captor to inspect the arguments passed to saveSyncedIntervals
            val captor = argumentCaptor<List<TimeInterval>>()
            verify(syncIntervalRepository, times(3)).saveSyncedIntervals(captor.capture())

            // Assert the specific states of the intervals after each save call
            val savedLists = captor.allValues
            // Check if a list representing the first batch save (end=130) was captured
            assertTrue(savedLists.any {
                it.size == 2 && it[0].end == 130L && it[1] == TimeInterval(
                    150,
                    200
                )
            })
            // Check if a list representing the subsequent save (end=140) was captured
            assertTrue(savedLists.any {
                it.size == 2 && it[0].end == 140L && it[1] == TimeInterval(
                    150,
                    200
                )
            })
            // Check if a list representing the final merged interval (end=200) was captured
            assertTrue(savedLists.any { it.size == 1 && it[0] == TimeInterval(0, 200) })
        }

        @Test
        @DisplayName("should handle cancellation during gap syncing")
        fun processNextTwoIntervals_cancellationDuringGap_throwsCancellationException() =
            runTest(testDispatcher) {
                // Arrange: Setup intervals and many photos to ensure batching and potential cancellation
                val initialIntervals = mutableListOf(TimeInterval(0, 100), TimeInterval(150, 200))
                val photosInGap = (1L..20L).map {
                    GalleryPhoto(
                        id = it,
                        dateAdded = 100L + it,
                        displayName = "photo$it.jpg",
                        path = mock<Uri>()
                    )
                }
                whenever(
                    galleryRepository.getPhotosInInterval(
                        any(),
                        any()
                    )
                ).thenReturn(photosInGap)

                // Mock saveSyncedIntervals to throw CancellationException after a few successful calls
                var saveCount = 0
                whenever(syncIntervalRepository.saveSyncedIntervals(any()))
                    .thenAnswer {
                        saveCount++
                        if (saveCount >= 2) { // Allow at least one batch save before faking cancellation
                            throw CancellationException("Test Cancellation")
                        }
                        Unit // Normal return for initial calls
                    }

                // Act & Assert: Launch the process in a separate coroutine within runTest's scope
                // This allows us to catch exceptions from the coroutine.
                val job = launch(coroutineContext) {
                    // Assert that a CancellationException is thrown when the mocked repository throws it
                    assertThrows(CancellationException::class.java) {
                        // Use an inner runTest for more precise control of the coroutineContext if needed,
                        // though for simple cancellation propagation, the outer launch is often sufficient.
                        runTest(testDispatcher) {
                            fullScanProcessManager.processNextTwoIntervals(
                                initialIntervals,
                                coroutineContext
                            )
                        }
                    }
                }
                advanceUntilIdle() // Allow the coroutine to run and potentially encounter the exception
                job.join() // Wait for the launched job to complete (either successfully or by throwing)

                // Verify interactions: ensure some operations occurred before cancellation
                verify(syncIntervalRepository, atLeastOnce()).saveSyncedIntervals(any())
                verify(galleryRepository).getPhotosInInterval(101, 149)
            }
    }

    @Nested
    @DisplayName("processTailEnd Tests")
    inner class ProcessTailEndTests {
        @Test
        @DisplayName("should sync photos in tail end when present")
        fun processTailEnd_withPhotosInTail_syncsCorrectly() = runTest(testDispatcher) {
            // Arrange: Setup an interval and mock photos existing after its end
            val initialIntervals = mutableListOf(TimeInterval(0, 100))
            val photosInTail = listOf(
                GalleryPhoto(
                    id = 5L,
                    dateAdded = 101L,
                    displayName = "tail1.jpg",
                    path = mock<Uri>()
                ),
                GalleryPhoto(
                    id = 6L,
                    dateAdded = 105L,
                    displayName = "tail2.jpg",
                    path = mock<Uri>()
                ),
                GalleryPhoto(
                    id = 7L,
                    dateAdded = 110L,
                    displayName = "tail3.jpg",
                    path = mock<Uri>()
                ),
                GalleryPhoto(
                    id = 8L,
                    dateAdded = 115L,
                    displayName = "tail4.jpg",
                    path = mock<Uri>()
                )
            )
            whenever(galleryRepository.getPhotos(startTimeSeconds = 101)).thenReturn(photosInTail)
            whenever(syncIntervalRepository.syncedIntervals).thenReturn(
                MutableStateFlow(
                    initialIntervals
                )
            )

            // Act
            val result = fullScanProcessManager.processTailEnd(initialIntervals, coroutineContext)
            advanceUntilIdle() // Allow batch processing to complete

            // Assert: Verify the interval's end is updated to the last synced photo's timestamp
            assertEquals(1, result.size)
            assertEquals(
                TimeInterval(0, 115),
                result[0]
            ) // Interval updated to end at last synced photo (115)

            // Verify interactions: galleryRepository was queried
            verify(galleryRepository).getPhotos(startTimeSeconds = 101)
            // Verify saveSyncedIntervals was called twice due to batching (batch size 3, 4 photos)
            verify(syncIntervalRepository, times(2)).saveSyncedIntervals(argThat {
                // Assert that lists representing the batch saves were captured
                (this.isNotEmpty() && this[0].end == 105L) || // After first batch (101, 105)
                        (this.isNotEmpty() && this[0].end == 115L)    // After remaining photos (110, 115)
            })
        }

        @Test
        @DisplayName("should do nothing if no photos in tail end")
        fun processTailEnd_noPhotosInTail_noChange() = runTest(testDispatcher) {
            // Arrange: Setup intervals but mock no photos found in the tail
            val initialIntervals = mutableListOf(TimeInterval(0, 100))
            whenever(galleryRepository.getPhotos(startTimeSeconds = 101)).thenReturn(emptyList())
            whenever(syncIntervalRepository.syncedIntervals).thenReturn(
                MutableStateFlow(
                    initialIntervals
                )
            )

            // Act
            val result = fullScanProcessManager.processTailEnd(initialIntervals, coroutineContext)
            advanceUntilIdle()

            // Assert: Verify the interval list remains unchanged
            assertEquals(1, result.size)
            assertEquals(TimeInterval(0, 100), result[0])
            // Verify galleryRepository was called but no save operation occurred
            verify(galleryRepository).getPhotos(startTimeSeconds = 101)
            verify(syncIntervalRepository, never()).saveSyncedIntervals(any())
        }

        @Test
        @DisplayName("should handle empty currentIntervals gracefully")
        fun processTailEnd_emptyIntervals_returnsEmpty() = runTest(testDispatcher) {
            // Arrange: Provide an empty list of current intervals
            val initialIntervals = mutableListOf<TimeInterval>()
            // No need to mock galleryRepository as it won't be called if intervals are empty

            // Act
            val result = fullScanProcessManager.processTailEnd(initialIntervals, coroutineContext)
            advanceUntilIdle()

            // Assert: Verify the result is an empty list and no repository interactions occurred
            assertTrue(result.isEmpty())
            verify(galleryRepository, never()).getPhotos(any())
            verify(syncIntervalRepository, never()).saveSyncedIntervals(any())
        }
    }
}
