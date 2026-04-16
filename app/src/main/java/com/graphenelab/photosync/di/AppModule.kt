package com.graphenelab.photosync.di

import com.graphenelab.photosync.manager.BackgroundSyncManager
import com.graphenelab.photosync.manager.DataCenterCloudManager
import com.graphenelab.photosync.manager.FullScanProcessManager
import com.graphenelab.photosync.manager.interfaces.IBackgroundSyncManager
import com.graphenelab.photosync.manager.interfaces.ICloudManager
import com.graphenelab.photosync.manager.interfaces.IFullScanProcessManager

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//application-wide singletons
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindBackgroundSyncManager(
        impl: BackgroundSyncManager
    ): IBackgroundSyncManager

    @Binds
    @Singleton
    abstract fun bindFullScanProcessManager(
        impl: FullScanProcessManager
    ): IFullScanProcessManager

    @Binds
    @Singleton
    abstract fun bindDataCenterCloudManager(
        impl: DataCenterCloudManager
    ): ICloudManager
}