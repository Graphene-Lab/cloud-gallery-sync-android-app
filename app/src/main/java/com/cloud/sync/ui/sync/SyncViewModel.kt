package com.cloud.sync.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.common.PhotoSyncStatusManager
import com.cloud.sync.common.SyncStatusManager
import com.cloud.sync.manager.interfaces.IBackgroundSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val backgroundSyncManager: IBackgroundSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        // Observe background worker status from the manager
        backgroundSyncManager.getPeriodicSyncWorkInfoFlow()
            .onEach { isScheduled ->
                _uiState.update { it.copy(isBackgroundSyncScheduled = isScheduled) }
            }
            .launchIn(viewModelScope) // Collects the flow as long as the ViewModel is active

        // Observe full scan progress from the service via SyncStatusManager
        viewModelScope.launch {
            SyncStatusManager.isSyncing.collect { isSyncing ->
                _uiState.update {
                    it.copy(
                        isFullScanInProgress = isSyncing
                    )
                }
            }
        }
        viewModelScope.launch {
            SyncStatusManager.successfulSyncPhotosCount.collect { count ->
                _uiState.update {
                    it.copy(
                        completedPhotos = count
                    )
                }
            }
        }
        viewModelScope.launch {
            SyncStatusManager.failedSyncPhotosCount.collect { count ->
                _uiState.update {
                    it.copy(
                        failedPhotos = count
                    )
                }
            }
        }
        viewModelScope.launch {
            SyncStatusManager.discoveredPhotosCount.collect { count ->
                _uiState.update {
                    it.copy(
                        totalPhotosToBeUploaded = count
                    )
                }
            }
        }
        viewModelScope.launch {
            PhotoSyncStatusManager.currentPhotoProgress.collect {progress ->
                _uiState.update {
                    it.copy(
                        progress = progress
                    )
                }
            }
        }
    }

    fun onFromNowSyncToggled(isEnabled: Boolean) {
        viewModelScope.launch {
            if (isEnabled) {
                backgroundSyncManager.schedulePeriodicSync()
            } else {
                backgroundSyncManager.cancelPeriodicSync()
            }
        }
    }

    fun startFullScan() {
        backgroundSyncManager.startFullScanService()
    }

    fun stopFullScan() {
        backgroundSyncManager.stopFullScanService()
    }
}