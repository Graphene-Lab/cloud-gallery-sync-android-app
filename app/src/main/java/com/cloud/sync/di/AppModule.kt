package com.cloud.sync.di

import com.cloud.communication.cryto.ZeroKnowledgeProof
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import com.cloud.sync.manager.BackgroundSyncManager
import com.cloud.sync.manager.FullScanProcessManager
import com.cloud.sync.manager.interfaces.IBackgroundSyncManager
import com.cloud.sync.manager.interfaces.IFullScanProcessManager

import dagger.Binds
import dagger.Module
import dagger.Provides
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
}