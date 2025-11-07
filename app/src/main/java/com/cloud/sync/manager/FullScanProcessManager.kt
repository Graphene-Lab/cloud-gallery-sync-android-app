package com.cloud.sync.manager

import android.content.ContentResolver
import android.util.Log
import com.cloud.communication.cryto.FileUploader
import com.cloud.communication.cryto.ZeroKnowledgeProof
import com.cloud.sync.BuildConfig
import com.cloud.sync.common.PhotoSyncStatusManager
import com.cloud.sync.common.PhotoSyncStatusManager.uploadedPhotosCount
import com.cloud.sync.common.SyncStatusManager
import com.cloud.sync.common.config.SyncConfig
import com.cloud.sync.domain.model.GalleryPhoto
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import com.cloud.sync.manager.interfaces.IFullScanProcessManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Concrete implementation of [com.cloud.sync.manager.interfaces.IFullScanProcessManager].
 * Manages the logic for a comprehensive gallery photo synchronization,
 * including interval management, photo fetching, batch syncing.
 */
class FullScanProcessManager @Inject constructor(
    private val syncIntervalRepository: ISyncRepository,
    private val galleryRepository: IGalleryRepository,
    private val syncConfig: SyncConfig,
    private val contentResolver: ContentResolver,
    private val zeroKnowledgeProof: ZeroKnowledgeProof?,


    ) : IFullScanProcessManager {

    private val concurrentLimit = Semaphore(2) // Allow max N concurrent operations

    override suspend fun initializeIntervals(): MutableList<TimeInterval> {
        if (BuildConfig.DEBUG) syncIntervalRepository.clearAllData()// TODO: Note - clears synced photos.
        val allIntervals = syncIntervalRepository.syncedIntervals.first().toMutableList()
        // Ensure the initial 0-timestamp interval exists for complete coverage.
        if (allIntervals.none { it.start == 0L }) {
            allIntervals.add(0, TimeInterval(0, 0))
        }
        allIntervals.sortBy { it.start }
        return allIntervals
    }

    override suspend fun processNextTwoIntervals(
        currentIntervals: MutableList<TimeInterval>,
        currentCoroutineContext: CoroutineContext
    ): MutableList<TimeInterval> {
        val interval1 = currentIntervals[0]
        val interval2 = currentIntervals[1]

        val photosInGap =
            galleryRepository.getPhotosInInterval(interval1.end + 1, interval2.start - 1)

        var tempInterval1 = interval1
        if (photosInGap.isNotEmpty()) {
            val onBatchSave: suspend (Long) -> Unit = { newEndTimestamp ->
                tempInterval1 = interval1.copy(end = newEndTimestamp)
                // Save current progress immediately for crash recovery.
                val updatedListForSave = currentIntervals.toMutableList()
                updatedListForSave[0] = tempInterval1
                syncIntervalRepository.saveSyncedIntervals(updatedListForSave)
            }
            SyncStatusManager.increaseDiscoveredPhotosCount(photosInGap.size)
            syncAndSaveInBatches(
                currentCoroutineContext,
                photosInGap,
                onBatchSave
            )
        }

        val mergedInterval = mergeTwoIntervals(tempInterval1, interval2)

        // Replace the two processed intervals with the newly merged one.
        val newList = currentIntervals.drop(2).toMutableList()
        newList.add(0, mergedInterval)

        syncIntervalRepository.saveSyncedIntervals(newList)
        return newList
    }

    override suspend fun processTailEnd(
        currentIntervals: MutableList<TimeInterval>,
        currentCoroutineContext: CoroutineContext
    ): MutableList<TimeInterval> {
        // No tail end to process if the list is empty (should not happen if initialized correctly).
        if (currentIntervals.isEmpty()) return currentIntervals

        val finalInterval = currentIntervals.first()
        // Fetch photos beyond the last synced timestamp.
        val photosInTail = galleryRepository.getPhotos(startTimeSeconds = finalInterval.end + 1)

        if (photosInTail.isNotEmpty()) {
            val onBatchSave: suspend (Long) -> Unit = { newEndTimestamp ->
                val updatedInterval = finalInterval.copy(end = newEndTimestamp)
                currentIntervals[0] = updatedInterval
                syncIntervalRepository.saveSyncedIntervals(currentIntervals)
            }
            SyncStatusManager.increaseDiscoveredPhotosCount(photosInTail.size)

            syncAndSaveInBatches(
                currentCoroutineContext,
                photosInTail,
                onBatchSave
            )
        }
        return currentIntervals
    }

    private fun mergeTwoIntervals(interval1: TimeInterval, interval2: TimeInterval): TimeInterval {
        return TimeInterval(
            interval1.start,
            maxOf(interval1.end, interval2.end)
        )
    }


    private suspend fun syncAndSaveInBatches(
    context: CoroutineContext,
    photos: List<GalleryPhoto>,
    onBatchSave: suspend (Long) -> Unit,
) = withContext(Dispatchers.IO) {
    val batchSize = syncConfig.batchSize
    var lastSyncedTimestamp = 0L

    // Process photos in parallel batches
    photos.chunked(batchSize).forEach { batch ->
        val deferredResults = batch.mapIndexed { index, photo ->
            async {
                concurrentLimit.withPermit {
                    context.ensureActive()

                    try {
                        // 1. Read original file content directly into memory
                        val originalBytes =
                            contentResolver.openInputStream(photo.path)?.use { inputStream ->
                                inputStream.readBytes()
                            } ?: throw IOException("Failed to read photo")

                        // 2. Generate encrypted filename from display name only
                        val encryptedFilename = zeroKnowledgeProof?.EncryptFullFileName(photo.displayName)

                        // 3. Encrypt bytes directly in memory
                        val encryptedBytes = zeroKnowledgeProof?.encryptBytes(
                            originalBytes,
                            photo.displayName,  // Use only filename, not path
                            photo.dateAdded     // Use photo's original timestamp
                        )

                        // 4. Upload encrypted bytes with encrypted filename
                        ByteArrayInputStream(encryptedBytes).use { encryptedStream ->
                            FileUploader.startSendFileWithProgressCallback(
                                encryptedStream,
                                encryptedFilename  // Server sees encrypted filename
                            ) { progress ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (progress.isCompleted) {
                                        PhotoSyncStatusManager.markPhotoCompleted(photo.displayName)
                                        SyncStatusManager.updateSuccessfulSyncPhotosCount()
                                        SyncStatusManager.turnOfSyncStatusBasedOnIfAllPhotosFetched()
                                    } else {
                                        PhotoSyncStatusManager.updatePhotoProgress(
                                            filename = photo.displayName, // Show original name in UI
                                            currentChunk = progress.currentChunk,
                                            totalChunks = progress.totalChunks
                                        )
                                    }
                                }
                            }
                        }
                        photo.dateAdded // Return timestamp for batch tracking
                    } catch (e: Exception) {
                        Log.e("PhotoSync", "Failed to encrypt ${photo.displayName}", e)
                        photo.dateAdded
                    }
                }
            }
        }

        // Wait for all photos in the batch to complete
        val timestamps = deferredResults.awaitAll()
        lastSyncedTimestamp = timestamps.maxOrNull() ?: lastSyncedTimestamp

        // Save batch progress
        withContext(Dispatchers.Main) {
            onBatchSave(lastSyncedTimestamp)
        }
    }
}
}