package com.graphenelab.photosync.manager.interfaces

import kotlinx.coroutines.flow.Flow

/**
 * Manages the scheduling and cancellation of background photo synchronization tasks,
 * and controls the foreground service for full scans.
 */
interface IBackgroundSyncManager {

    /**
     * Schedules a periodic photo synchronization task to run at a specified interval.
     *
     * If this is the very first time the feature is enabled, it sets the current time
     * as the "sync from now" anchor point, ensuring subsequent syncs only process newer photos.
     * The task requires an active network connection.
     * Uses WorkManager's [androidx.work.ExistingPeriodicWorkPolicy.KEEP] to avoid re-enqueuing if already active.
     */
    suspend fun schedulePeriodicSync()

    /**
     * Cancels the currently scheduled periodic photo synchronization task.
     *
     * Also deletes the saved "sync from now" anchor point, effectively resetting
     * the "from now" sync feature.
     */
    suspend fun cancelPeriodicSync()

    /**
     * Initiates a full photo scan by starting the [com.graphenelab.photosync.background.FullScanService] as a foreground service.
     * This operation processes all existing photos on the device. For efficiency, already processed photos intervals are stored in storage and not processed again.
     */
    fun startFullScanService()

    /**
     * Stops the currently running [com.graphenelab.photosync.background.PhotoSyncWorker], halting any ongoing full scan.
     */
    fun stopFullScanService()

    /**
     * Provides a [Flow] that emits `true` if the periodic photo synchronization worker
     * is currently enqueued or running, and `false` otherwise.
     *
     * Consumers can collect this flow to observe the background sync status.
     *
     * @return A [Flow] of [Boolean] indicating whether the periodic sync is active.
     */
    fun getPeriodicSyncWorkInfoFlow(): Flow<Boolean>
}