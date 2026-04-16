package com.graphenelab.photosync.domain.repositroy

interface ICseMasterKeyRepository {
    fun saveKey(key: ByteArray)
    fun getKey(): ByteArray?
    fun hasKey(): Boolean
    fun clearKey()
}