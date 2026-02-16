package com.cloud.sync.di

import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.cloud.sync.common.config.SyncConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.SharedPreferences

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    private const val PREFS_NAME = "SyncAppPrefs"
    private const val KEY_ENCRYPTION_ENABLED = "is_encryption_enabled"
    private const val KEY_PHOTO_FOLDER_PATH = "photo_folder_path"

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideSyncConfig(sharedPreferences: SharedPreferences): SyncConfig {
        val isEncryptionEnabled = sharedPreferences.getBoolean(KEY_ENCRYPTION_ENABLED, true)
        val photoFolderPath = sharedPreferences.getString(KEY_PHOTO_FOLDER_PATH, "Photos/") ?: "Photos/"
        return SyncConfig(isEncryptionEnabled = isEncryptionEnabled, photoFolderPath = photoFolderPath)
    }

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
}
