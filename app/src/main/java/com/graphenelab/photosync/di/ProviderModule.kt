package com.graphenelab.photosync.di

import com.graphenelab.communication.crypto.IZeroKnowledgeProof
import com.graphenelab.communication.crypto.ZeroKnowledgeProof
import com.graphenelab.photosync.domain.repositroy.ICseMasterKeyRepository
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
    fun provideZeroKnowledgeProof(cseMasterKeyRepository: ICseMasterKeyRepository): IZeroKnowledgeProof? {
        val masterKey = cseMasterKeyRepository.getKey()
        return if (masterKey != null) {
            ZeroKnowledgeProof(masterKey)
        } else {
            null
        }
    }
}
