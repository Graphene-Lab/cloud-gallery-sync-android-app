package com.cloud.sync.domain.model

/**
 * Represents the progress of a synchronization process.
 */
data class SyncPhotosProgress(
    val photosSuccessfullySynced: Int = 0,
    val photosFailedToSynced: Int = 0,
)