package com.cloud.sync.domain.repositroy

interface IAppSettingsRepository {
    fun isEncryptionEnabled(): Boolean
    fun setEncryptionEnabled(enabled: Boolean)
    fun getPhotoFolderPath(): String
}
