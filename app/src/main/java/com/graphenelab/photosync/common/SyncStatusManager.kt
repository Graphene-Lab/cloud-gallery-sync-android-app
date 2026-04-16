package com.graphenelab.photosync.common

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

    private var _noPhotosFoundToSync: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val noPhotosFoundToSync: StateFlow<Boolean> = _noPhotosFoundToSync

    private var _totalProcessedPhotosCount: Int = 0

    fun updateSyncStatus(isSyncing: Boolean) {
        _isSyncing.value = isSyncing
    }

    fun updateNoPhotosFoundToSync(noPhotosFoundToSync: Boolean) {
        _noPhotosFoundToSync.value = noPhotosFoundToSync
    }

    fun resetSyncSession() {
        _isSyncing.value = false
        _successfulSyncPhotosCount.value = 0
        _failedSyncPhotosCount.value = 0
        _discoveredPhotosCount.value = 0
        _noPhotosFoundToSync.value = false
        _totalProcessedPhotosCount = 0
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

    fun incrementFailedSyncPhotosCount() {
        _failedSyncPhotosCount.value++
        _totalProcessedPhotosCount =
            _successfulSyncPhotosCount.value + _failedSyncPhotosCount.value
    }

    fun increaseDiscoveredPhotosCount(countToIncrease: Int) {
        _discoveredPhotosCount.value += countToIncrease
    }
}
