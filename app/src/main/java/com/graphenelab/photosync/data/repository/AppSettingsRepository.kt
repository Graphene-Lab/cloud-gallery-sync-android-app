package com.graphenelab.photosync.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.graphenelab.photosync.domain.repositroy.IAppSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : IAppSettingsRepository {

    companion object {
        private const val KEY_ENCRYPTION_ENABLED = "is_encryption_enabled"
        private const val KEY_PHOTO_FOLDER_PATH = "photo_folder_path"
        private const val DEFAULT_PHOTO_FOLDER_PATH = "Photos/"
    }

    override fun isEncryptionEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ENCRYPTION_ENABLED, true)
    }

    override fun setEncryptionEnabled(enabled: Boolean) {
        sharedPreferences.edit(commit = true) {
            putBoolean(KEY_ENCRYPTION_ENABLED, enabled)
        }
    }

    override fun getPhotoFolderPath(): String {
        return sharedPreferences.getString(
            KEY_PHOTO_FOLDER_PATH,
            DEFAULT_PHOTO_FOLDER_PATH
        ) ?: DEFAULT_PHOTO_FOLDER_PATH
    }
}
