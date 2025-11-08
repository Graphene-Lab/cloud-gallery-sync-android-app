package com.cloud.sync.di

import com.cloud.sync.manager.BackgroundSyncManager
import com.cloud.sync.manager.DataCenterCloudManager
import com.cloud.sync.manager.FullScanProcessManager
import com.cloud.sync.manager.interfaces.IBackgroundSyncManager
import com.cloud.sync.manager.interfaces.ICloudManager
import com.cloud.sync.manager.interfaces.IFullScanProcessManager

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