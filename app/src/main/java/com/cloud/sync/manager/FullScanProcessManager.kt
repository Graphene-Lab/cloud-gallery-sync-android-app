package com.cloud.sync.manager

import android.content.ContentResolver
import com.cloud.communication.cryto.FileUploader
import com.cloud.sync.common.SyncStatusManager
import com.cloud.sync.common.config.SyncConfig
import com.cloud.sync.domain.model.GalleryPhoto
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import com.cloud.sync.manager.interfaces.IFullScanProcessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Concrete implementation of [com.cloud.sync.manager.interfaces.IFullScanProcessManager].
 * Manages the logic for a comprehensive gallery photo synchronization,
 * including interval management, photo fetching, batch syncing.
 */
class FullScanProcessManager @Inject constructor(
    private val syncIntervalRepository: ISyncRepository,
    private val galleryRepository: IGalleryRepository,
    private val syncConfig: SyncConfig,
    private val contentResolver: ContentResolver


) : IFullScanProcessManager {

    override suspend fun initializeIntervals(): MutableList<TimeInterval> {
        syncIntervalRepository.clearAllData()// TODO: Remove before production.
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
            syncAndSaveInBatches(
                currentCoroutineContext,
                photosInGap,
                "Syncing gap...",
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
            syncAndSaveInBatches(
                currentCoroutineContext,
                photosInTail,
                "Finalizing...",
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
        statusPrefix: String,
        onBatchSave: suspend (Long) -> Unit
    ) = withContext(Dispatchers.IO) { // Move the entire function to IO dispatcher
        val batchSize = syncConfig.batchSize
        var photosInBatch = 0
        var lastSyncedTimestamp = 0L

        photos.forEachIndexed { index, photo ->
            context.ensureActive() // Check for parent coroutine cancellation.

            try {
                contentResolver.openInputStream(photo.path)?.use { inputStream ->
                    // Read all bytes immediately to avoid stream closing issues
                    val bytes = inputStream.readBytes()
                    // Create a new ByteArrayInputStream for the FileUploader
                    java.io.ByteArrayInputStream(bytes).use { byteStream ->
                        FileUploader.startSendFile(
                            byteStream,
                            photo.displayName
                        ) // Use sync version
                    }
                }
            } catch (e: Exception) {
                println("Failed to upload ${photo.displayName}: ${e.message}")
            }

            lastSyncedTimestamp = photo.dateAdded

            // Update status on main thread
            withContext(Dispatchers.Main) {
                SyncStatusManager.update(true, "$statusPrefix (${index + 1}/${photos.size})")
            }

            if (++photosInBatch >= batchSize) {
                // Switch to main context for saving intervals
                withContext(Dispatchers.Main) {
                    onBatchSave(lastSyncedTimestamp)
                }
                photosInBatch = 0
            }
        }

        if (photosInBatch > 0) {
            withContext(Dispatchers.Main) {
                onBatchSave(lastSyncedTimestamp)
            }
        }
    }
}