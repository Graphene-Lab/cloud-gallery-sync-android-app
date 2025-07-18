package com.cloud.sync.di

import com.cloud.sync.data.local.secure.TokenStorage
import com.cloud.sync.data.local.datastore.SyncPreferencesDataSource
import com.cloud.sync.data.local.mediastore.PhotoLocalDataSource
import com.cloud.sync.data.network.payment.PaymentService
import com.cloud.sync.data.network.user.CloudSpaceService
import com.cloud.sync.data.repository.CloudSpaceRepository
import com.cloud.sync.data.repository.GalleryRepositoryImpl
import com.cloud.sync.data.repository.OauthTokenRepository
import com.cloud.sync.data.repository.PaymentRepository
import com.cloud.sync.data.repository.SyncRepositoryImpl
import com.cloud.sync.domain.repositroy.ICloudSpaceRepository
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.IOauthTokenRepository
import com.cloud.sync.domain.repositroy.IPaymentRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGalleryRepository(
        photoLocalDataSource: PhotoLocalDataSource
    ): IGalleryRepository {
        return GalleryRepositoryImpl(photoLocalDataSource)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        syncPreferencesDataSource: SyncPreferencesDataSource
    ): ISyncRepository {
        return SyncRepositoryImpl(syncPreferencesDataSource)
    }

    @Provides
    @Singleton
    fun provideOauthTokenRepository(
        tokenStorage: TokenStorage
    ): IOauthTokenRepository {
        return OauthTokenRepository(tokenStorage)
    }

    @Provides
    @Singleton
    fun providePaymentRepository(
        paymentService: PaymentService
    ): IPaymentRepository {
        return PaymentRepository(paymentService)
    }

    @Provides
    @Singleton
    fun provideCloudSpaceRepository(
        cloudSpaceService: CloudSpaceService
    ): ICloudSpaceRepository {
        return CloudSpaceRepository(cloudSpaceService)
    }
}