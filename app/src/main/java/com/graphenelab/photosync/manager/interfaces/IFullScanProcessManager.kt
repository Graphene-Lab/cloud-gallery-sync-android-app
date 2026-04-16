package com.graphenelab.photosync.manager.interfaces

import com.graphenelab.photosync.domain.model.TimeInterval
import kotlin.coroutines.CoroutineContext

/**
 * Defines the contract for managing a full gallery scan and synchronization process.
 * This manager orchestrates the identification, synchronization, and tracking of photo
 * synchronization progress across various time intervals.
 */
interface IFullScanProcessManager {

    /**
     * Initializes and retrieves the current list of synchronized time intervals from storage.
     * Ensures a base (0,0) interval exists to handle synchronization from the beginning of time.
     * @return A mutable, sorted list of [TimeInterval] objects representing synced periods.
     */
    suspend fun initializeIntervals(): MutableList<TimeInterval>

    /**
     * Processes the gap between the first two unsynced intervals, synchronizes any photos found
     * (note: updates [com.graphenelab.photosync.common.SyncStatusManager]) after each uploaded photo,],
     * and merges these intervals into a single, contiguous synced interval.
     * This method is a core step in progressively syncing the gallery timeline.
     * @param currentIntervals The mutable list of current synchronization intervals.
     * @param currentCoroutineContext The [CoroutineContext] for cancellation checks during processing.
     * @return The updated list of [TimeInterval]s after processing and merging the first two.
     */
    suspend fun processNextTwoIntervals(
        currentIntervals: MutableList<TimeInterval>,
        currentCoroutineContext: CoroutineContext
    ): MutableList<TimeInterval>

    /**
     * Handles the synchronization of photos that exist beyond the last known synced interval
     * (the "tail end" of the gallery timeline).
     * @param currentIntervals The mutable list containing the final synced interval.
     * @param currentCoroutineContext The [CoroutineContext] for cancellation checks during processing.
     * @return The updated list of [TimeInterval]s after extending the last interval to cover new photos.
     */
    suspend fun processTailEnd(
        currentIntervals: MutableList<TimeInterval>,
        currentCoroutineContext: CoroutineContext
    ): MutableList<TimeInterval>

}