package com.cloud.sync.ui.sync

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.common.PhotoSyncStatusManager
import com.cloud.sync.common.SyncStatusManager
import com.cloud.sync.manager.PermissionSet
import com.cloud.sync.manager.interfaces.IBackgroundSyncManager
import com.cloud.sync.manager.interfaces.IPermissionsManager
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
    private val backgroundSyncManager: IBackgroundSyncManager,
    private val permissionsManager: IPermissionsManager
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
            PhotoSyncStatusManager.currentPhotoProgress.collect { progress ->
                _uiState.update {
                    it.copy(
                        progress = progress
                    )
                }
            }
        }
    }

    fun onFromNowSyncToggled(isEnabled: Boolean) {
        _uiState.update { it.copy(syncFromNowButtonClicked = true) }
        viewModelScope.launch {
            if (isEnabled) {
                backgroundSyncManager.schedulePeriodicSync()
            } else {
                backgroundSyncManager.cancelPeriodicSync()
            }
        }
    }

    fun onStartFullScanButtonClicked(context: Context) {
        _uiState.update { it.copy(startFullScanButtonClicked = true) }
        if (permissionsManager.hasPermissions(context, PermissionSet.SyncEssentials)) {
            startFullScan()

        } else {
            requestSyncPermissions()
        }
    }

    private fun requestSyncPermissions() {
        permissionsManager.requestPermissions(PermissionSet.SyncEssentials)
    }

    fun startFullScan() {
        _uiState.update {
            it.copy(
                permissionDenied = false,
            )
        }
        backgroundSyncManager.startFullScanService()
    }

    fun stopFullScan() {
        backgroundSyncManager.stopFullScanService()
    }

    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionsManager.setLauncher(launcher)
    }

    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        Log.d("SyncViewModel", "handlePermissionResult: ${permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissions.getOrDefault(
                    Manifest.permission.POST_NOTIFICATIONS, false
                ) || permissions.getOrDefault(
                    Manifest.permission.READ_MEDIA_IMAGES, false
                ) || permissions.getOrDefault(
                    Manifest.permission.READ_MEDIA_VIDEO, false
                )
            ) {
                if (_uiState.value.startFullScanButtonClicked) {
                    startFullScan()
                } else if (_uiState.value.syncFromNowButtonClicked) {
                    onFromNowSyncToggled(true)
                }

                _uiState.update {
                    it.copy(
                        permissionDenied = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        permissionDenied = true,
                    )
                }
            }
        } else {
            if (permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)) {
                Log.d("SyncViewModel", "Permission granted")
                if (_uiState.value.startFullScanButtonClicked) {
                    Log.d("SyncViewModel", "Permission 1")

                    startFullScan()
                } else if (_uiState.value.syncFromNowButtonClicked) {
                    Log.d("SyncViewModel", "Permission 2")

                    onFromNowSyncToggled(true)
                }
            }
            else{
                _uiState.update {
                    it.copy(
                        permissionDenied = true,
                    )
                }
            }
        }


    }

}