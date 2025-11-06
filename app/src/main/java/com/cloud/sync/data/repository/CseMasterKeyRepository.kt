package com.cloud.sync.data.repository

import com.cloud.sync.data.local.secure.SecureCseMasterKeyStorage
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
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