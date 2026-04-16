package com.graphenelab.photosync.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the sync status of individual photos during upload process.
 * Tracks progress per photo with chunk-level granularity.
 */
object PhotoSyncStatusManager {
    
    data class PhotoProgress(
        val filename: String,
        val currentChunk: Int,
        val totalChunks: Int,
        val isCompleted: Boolean = false,
        val hasError: Boolean = false,
        val errorMessage: String? = null
    ) {
        val progressPercentage: Int
            get() = if (totalChunks > 0) (currentChunk * 100) / totalChunks else 0
    }
    
    private val _currentPhotoProgress = MutableStateFlow<PhotoProgress?>(null)
    val currentPhotoProgress: StateFlow<PhotoProgress?> = _currentPhotoProgress.asStateFlow()
    
    private val _allPhotosProgress = MutableStateFlow<Map<String, PhotoProgress>>(emptyMap())
    val allPhotosProgress: StateFlow<Map<String, PhotoProgress>> = _allPhotosProgress.asStateFlow()
    
    /**
     * Updates progress for a specific photo
     */
    fun updatePhotoProgress(
        filename: String, 
        currentChunk: Int, 
        totalChunks: Int,
        isCompleted: Boolean = false,
        hasError: Boolean = false,
        errorMessage: String? = null
    ) {
        val progress = PhotoProgress(
            filename = filename,
            currentChunk = currentChunk,
            totalChunks = totalChunks,
            isCompleted = isCompleted,
            hasError = hasError,
            errorMessage = errorMessage
        )
        
        // Update individual photo progress
        _currentPhotoProgress.value = progress
        
        // Update all photos map
        val currentMap = _allPhotosProgress.value.toMutableMap()
        currentMap[filename] = progress
        _allPhotosProgress.value = currentMap
    }
    
    /**
     * Marks a photo as completed
     */
    fun markPhotoCompleted(filename: String) {
        val currentProgress = _allPhotosProgress.value[filename]
        if (currentProgress != null) {
            updatePhotoProgress(
                filename = filename,
                currentChunk = currentProgress.totalChunks,
                totalChunks = currentProgress.totalChunks,
                isCompleted = true
            )
        }
    }
    
    /**
     * Marks a photo as failed
     */
    fun markPhotoFailed(filename: String, errorMessage: String) {
        val currentProgress = _allPhotosProgress.value[filename]
        if (currentProgress != null) {
            updatePhotoProgress(
                filename = filename,
                currentChunk = currentProgress.currentChunk,
                totalChunks = currentProgress.totalChunks,
                hasError = true,
                errorMessage = errorMessage
            )
        }
    }
    
    /**
     * Clears all progress data
     */
    fun clearAll() {
        _currentPhotoProgress.value = null
        _allPhotosProgress.value = emptyMap()
    }
    
    /**
     * Removes progress for a specific photo
     */
    fun removePhoto(filename: String) {
        val currentMap = _allPhotosProgress.value.toMutableMap()
        currentMap.remove(filename)
        _allPhotosProgress.value = currentMap
        
        // Clear current if it's the removed photo
        if (_currentPhotoProgress.value?.filename == filename) {
            _currentPhotoProgress.value = null
        }
    }

    fun isAllPhotosCompleted(): Boolean {
        return _allPhotosProgress.value.values.all { it.isCompleted }
    }

    //
    fun uploadedPhotosCount(): Int{
        return  _allPhotosProgress.value.values.count { it.isCompleted }
    }
}
