package com.graphenelab.photosync.ui.scan

import android.Manifest
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graphenelab.photosync.manager.interfaces.IPermissionsManager
import com.graphenelab.photosync.manager.interfaces.IQRScanner
import com.graphenelab.photosync.manager.PermissionSet
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val permissionsManager: IPermissionsManager,
    private val qrScanner: IQRScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState

    fun setScanLauncher(launcher: ActivityResultLauncher<ScanOptions>) {
        qrScanner.setLauncher(launcher)
    }

    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionsManager.setLauncher(launcher)
    }

    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        if (permissions.getOrDefault(Manifest.permission.CAMERA, false)) {
            startQRScanner()
        } else {
            viewModelScope.launch {
                _uiState.emit(ScanUiState.PermissionDenied)
            }
        }
    }

    fun handleScanResult(result: String?) {
        viewModelScope.launch {
            _uiState.emit(ScanUiState.Scanned(result))
        }
    }

    fun onScanButtonClicked(context: Context) {
        if (permissionsManager.hasPermissions(context, PermissionSet.Camera)) {
            startQRScanner()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        permissionsManager.requestPermissions(PermissionSet.Camera)
    }

    private fun startQRScanner() {
        qrScanner.startScan()
    }
}