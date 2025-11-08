package com.cloud.sync.di

import com.cloud.communication.cryto.IZeroKnowledgeProof
import com.cloud.communication.cryto.ZeroKnowledgeProof
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideZeroKnowledgeProof(cseMasterKeyRepository: ICseMasterKeyRepository): IZeroKnowledgeProof {
        val masterKey = cseMasterKeyRepository.getKey()
        return ZeroKnowledgeProof(masterKey)
    }
}
