package com.graphenelab.photosync.di

import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.graphenelab.photosync.common.config.SyncConfig
import com.graphenelab.photosync.domain.repositroy.IAppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.SharedPreferences

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    private const val PREFS_NAME = "SyncAppPrefs"

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideSyncConfig(appSettingsRepository: IAppSettingsRepository): SyncConfig {
        return SyncConfig(
            isEncryptionEnabled = appSettingsRepository.isEncryptionEnabled(),
            photoFolderPath = appSettingsRepository.getPhotoFolderPath()
        )
    }

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
}
