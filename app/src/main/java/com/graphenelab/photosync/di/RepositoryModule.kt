package com.graphenelab.photosync.di

import com.graphenelab.photosync.data.local.secure.TokenStorage
import com.graphenelab.photosync.data.local.datastore.SyncPreferencesDataSource
import com.graphenelab.photosync.data.local.mediastore.PhotoLocalDataSource
import com.graphenelab.photosync.data.local.secure.SessionRepository
import com.graphenelab.photosync.data.network.payment.PaymentService
import com.graphenelab.photosync.data.network.user.CloudSpaceService
import com.graphenelab.photosync.data.repository.CloudSpaceRepository
import com.graphenelab.photosync.data.local.secure.SecureCseMasterKeyStorage
import com.graphenelab.photosync.data.repository.AppSettingsRepository
import com.graphenelab.photosync.data.repository.GalleryRepositoryImpl
import com.graphenelab.photosync.data.repository.OauthTokenRepository
import com.graphenelab.photosync.data.repository.PaymentRepository
import com.graphenelab.photosync.data.repository.CseMasterKeyRepository
import com.graphenelab.photosync.data.repository.SyncRepositoryImpl
import com.graphenelab.photosync.domain.repositroy.IAppSettingsRepository
import com.graphenelab.photosync.domain.repositroy.ICloudSpaceRepository
import com.graphenelab.photosync.domain.repositroy.IGalleryRepository
import com.graphenelab.photosync.domain.repositroy.IOauthTokenRepository
import com.graphenelab.photosync.domain.repositroy.IPaymentRepository
import com.graphenelab.photosync.domain.repositroy.ISessionRepository
import com.graphenelab.photosync.domain.repositroy.ICseMasterKeyRepository
import com.graphenelab.photosync.domain.repositroy.ISyncRepository
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
    fun provideKeyRepository(
        secureCseMasterKeyStorage: SecureCseMasterKeyStorage
    ): ICseMasterKeyRepository {
        return CseMasterKeyRepository(secureCseMasterKeyStorage)
    }

    @Provides
    @Singleton
    fun provideCloudSpaceRepository(
        cloudSpaceService: CloudSpaceService
    ): ICloudSpaceRepository {
        return CloudSpaceRepository(cloudSpaceService)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionRepository: SessionRepository
    ): ISessionRepository {
        return sessionRepository
    }

    @Provides
    @Singleton
    fun provideAppSettingsRepository(
        appSettingsRepository: AppSettingsRepository
    ): IAppSettingsRepository {
        return appSettingsRepository
    }
}
