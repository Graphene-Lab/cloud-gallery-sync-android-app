package com.cloud.sync.ui.sync

import com.cloud.sync.common.PhotoSyncStatusManager

// UI State class for the ViewModel
data class SyncUiState(
    val isFullScanInProgress: Boolean = false,
    val isBackgroundSyncScheduled: Boolean = false,
    val statusText: String = "Ready.",
    val completedPhotos: Int = 0,
    val totalPhotosToBeUploaded: Int = 0,
    val failedPhotos: Int = 0,
    val progress: PhotoSyncStatusManager.PhotoProgress? = null,
    val permissionDenied: Boolean = false,
    val startFullScanButtonClicked: Boolean = false,
    val syncFromNowButtonClicked: Boolean = false,
    val noPhotosToSync: Boolean = false
)