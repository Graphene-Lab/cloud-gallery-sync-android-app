package com.cloud.sync.background

import android.content.Context
import android.content.ContentResolver
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cloud.communication.cryto.IZeroKnowledgeProof
import com.cloud.sync.common.UploadTimeouts
import com.cloud.sync.common.config.SyncConfig
import com.cloud.sync.domain.model.GalleryPhoto
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import com.cloud.sync.manager.interfaces.ICloudManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

@HiltWorker
class PhotoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: ISyncRepository,
    private val galleryRepository: IGalleryRepository,
    private val syncConfig: SyncConfig,
    private val contentResolver: ContentResolver,
    private val zeroKnowledgeProof: IZeroKnowledgeProof?,
    private val dataCenterCloudManager: ICloudManager
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        private const val TAG = "PhotoSyncWorker";
    }

    override suspend fun doWork(): Result {
        println("Worker: Starting periodic 'From Now' sync check.")
        try {
            val syncPoint = syncRepository.syncFromNowPoint.first()
            if (syncPoint == 0L) {
                println("Worker: 'Sync From Now' is not set up. Skipping.")
                return Result.success()
            }

            val allIntervals = syncRepository.syncedIntervals.first().toMutableList()
            val fromNowIntervalIndex = allIntervals.indexOfFirst { it.start == syncPoint }
            if (fromNowIntervalIndex == -1) {
                println("Worker: Error! Anchor interval not found. Failing job.")
                return Result.failure()
            }

            val fromNowInterval = allIntervals[fromNowIntervalIndex]
            val photosToSync =
                galleryRepository.getPhotos(startTimeSeconds = fromNowInterval.end + 1)
            if (photosToSync.isEmpty()) {
                println("Worker: No new photos found.")
                return Result.success()
            }

            println("Worker: Found ${photosToSync.size} new photos. Starting upload...")

            val onBatchSave: suspend (Long) -> Unit = { newTimestamp ->
                val updatedInterval = fromNowInterval.copy(end = newTimestamp)
                allIntervals[fromNowIntervalIndex] = updatedInterval
                syncRepository.saveSyncedIntervals(allIntervals)
                println("Worker: Saved batch progress. New end is $newTimestamp")
            }

            syncAndSaveInBatches(
                photos = photosToSync,
                initialTimestamp = fromNowInterval.end,
                onBatchSave = onBatchSave
            )

            return Result.success()
        } catch (e: Exception) {
            println("Worker: Sync failed with exception: ${e.message}")
            return Result.retry()
        }
    }

    private suspend fun syncAndSaveInBatches(
        photos: List<GalleryPhoto>,
        initialTimestamp: Long,
        onBatchSave: suspend (Long) -> Unit
    ) = coroutineScope {
        val batchSize = syncConfig.batchSize
        var photosInBatch = 0
        var lastSyncedTimestamp = initialTimestamp

        for (photo in photos) {
            ensureActive()

            withContext(Dispatchers.IO) {
                try {
                    // 1. Read original file content directly into memory
                    val originalBytes =
                        contentResolver.openInputStream(photo.path)?.use { inputStream ->
                            inputStream.readBytes()
                        } ?: throw IOException("Failed to read photo")

                    val fileContentToUpload: ByteArray
                    val fileNameToUpload: String
                    val clearFullPath = syncConfig.photoFolderPath + photo.displayName

                    if (syncConfig.isEncryptionEnabled && zeroKnowledgeProof != null) {
                        // 2. Encrypt the full transport path, not only the filename.
                        fileNameToUpload = zeroKnowledgeProof.EncryptFullFileName(clearFullPath)

                        // 3. Derive the content key from the clear full path
                        fileContentToUpload = zeroKnowledgeProof.encryptBytes(
                            originalBytes,
                            clearFullPath,
                            photo.lastModifiedSeconds
                        )
                    } else {
                        // Skip encryption
                        fileNameToUpload = clearFullPath
                        fileContentToUpload = originalBytes
                    }

                    // 4. Upload bytes with the chosen filename (encrypted or original)
                    val uploadCompleted = withTimeoutOrNull(
                        UploadTimeouts.forSizeBytes(fileContentToUpload.size)
                    ) {
                        suspendCancellableCoroutine { continuation ->
                            val finished = AtomicBoolean(false)
                            dataCenterCloudManager.uploadFileBytes(
                                fileContentToUpload,
                                fileNameToUpload,
                                photo.lastModifiedSeconds
                            ) { progress ->
                                // Progress is handled in background, just log
                                if (progress.hasError) {
                                    Log.w(
                                        TAG,
                                        "Worker: Failed upload callback for ${photo.displayName}: " +
                                            (progress.errorMessage ?: "Unknown upload error")
                                    )
                                    if (finished.compareAndSet(false, true) && continuation.isActive) {
                                        continuation.resume(Unit)
                                    }
                                } else if (progress.isCompleted) {
                                    Log.d(TAG, "Worker: Completed upload for ${photo.displayName}")
                                    if (finished.compareAndSet(false, true) && continuation.isActive) {
                                        continuation.resume(Unit)
                                    }
                                }
                            }
                        }
                    }
                    if (uploadCompleted == null) {
                        println("Worker: Upload timed out for ${photo.displayName}")
                    }
                    lastSyncedTimestamp = photo.dateAdded
                    photosInBatch++
                } catch (e: Exception) {
                    println("Worker: Failed to upload ${photo.displayName}: ${e.message}")
                    lastSyncedTimestamp = photo.dateAdded
                    photosInBatch++
                }
            }

            if (photosInBatch >= batchSize) {
                onBatchSave(lastSyncedTimestamp)
                photosInBatch = 0
            }
        }

        if (photosInBatch > 0) {
            onBatchSave(lastSyncedTimestamp)
        }
    }
}
