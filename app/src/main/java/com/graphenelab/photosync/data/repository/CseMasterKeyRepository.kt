package com.graphenelab.photosync.data.repository

import com.graphenelab.photosync.data.local.secure.SecureCseMasterKeyStorage
import com.graphenelab.photosync.domain.repositroy.ICseMasterKeyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CseMasterKeyRepository @Inject constructor(
    private val secureCseMasterKeyStorage: SecureCseMasterKeyStorage
) : ICseMasterKeyRepository {

    override fun saveKey(key: ByteArray) {
        secureCseMasterKeyStorage.saveKey(key)
    }

    override fun getKey(): ByteArray? {
        return secureCseMasterKeyStorage.getKey()
    }

    override fun hasKey(): Boolean {
        return secureCseMasterKeyStorage.hasKey()
    }
    override fun clearKey() {
        secureCseMasterKeyStorage.clearKey()
    }
}