package com.cloud.sync.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A singleton object to manage and broadcast the state of the full scan.
 * The Service writes to it, and the ViewModel reads from it.
 * Implementation of Observable pattern.
 */
object SyncStatusManager {
    private val _isSyncing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _successfulSyncPhotosCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val successfulSyncPhotosCount: StateFlow<Int> = _successfulSyncPhotosCount

    private val _failedSyncPhotosCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val failedSyncPhotosCount: StateFlow<Int> = _failedSyncPhotosCount

    private var _discoveredPhotosCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val discoveredPhotosCount: StateFlow<Int> = _discoveredPhotosCount

    private var _totalProcessedPhotosCount: Int = 0

    fun updateSyncStatus(isSyncing: Boolean) {
        _isSyncing.value = isSyncing
    }

    fun turnOfSyncStatusBasedOnIfAllPhotosFetched() {
        if (_discoveredPhotosCount.value <= (_successfulSyncPhotosCount.value + _failedSyncPhotosCount.value))
            _isSyncing.value = false
    }

    //TODO: add _totalPhotosCount calculation to separate function
    fun updateSuccessfulSyncPhotosCount() {
        _successfulSyncPhotosCount.value++
        _totalProcessedPhotosCount =
            _successfulSyncPhotosCount.value + _failedSyncPhotosCount.value;
    }

    fun updateFailedSyncPhotosCount(count: Int) {
        _failedSyncPhotosCount.value = count
        _totalProcessedPhotosCount =
            _successfulSyncPhotosCount.value + _failedSyncPhotosCount.value;
    }

    fun increaseDiscoveredPhotosCount(countToIncrease: Int) {
        _discoveredPhotosCount.value += countToIncrease
    }
}