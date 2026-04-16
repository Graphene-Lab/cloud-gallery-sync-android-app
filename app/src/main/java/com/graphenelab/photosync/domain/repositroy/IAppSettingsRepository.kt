package com.graphenelab.photosync.domain.repositroy

interface IAppSettingsRepository {
    fun isEncryptionEnabled(): Boolean
    fun setEncryptionEnabled(enabled: Boolean)
    fun getPhotoFolderPath(): String
}
