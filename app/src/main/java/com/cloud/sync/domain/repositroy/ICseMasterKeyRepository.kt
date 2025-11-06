package com.cloud.sync.domain.repositroy

interface ICseMasterKeyRepository {
    fun saveKey(key: ByteArray)
    fun getKey(): ByteArray?
    fun hasKey(): Boolean
    fun clearKey()
}