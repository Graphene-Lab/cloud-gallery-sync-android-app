package com.graphenelab.photosync.data.local.secure

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

@Singleton
class SecureCseMasterKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREF_FILE_NAME = "secure_app_settings"
        private const val CLIENT_SIDE_ENCRYPTION_MASTER_KEY_PREF_KEY = "client_side_encryption_master_key"
    }

    // This generates/retrieves the master key from Android KeyStore that EncryptedSharedPreferences uses internally.
    private val epMasterKeyAlias by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    private val sharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        EncryptedSharedPreferences.create(
            PREF_FILE_NAME,
            epMasterKeyAlias, // This references the KeyStore-managed master key for EncryptedSharedPreferences itself
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveKey(key: ByteArray) {
        val encodedKey = android.util.Base64.encodeToString(key, android.util.Base64.NO_WRAP)
        sharedPreferences.edit { putString(CLIENT_SIDE_ENCRYPTION_MASTER_KEY_PREF_KEY, encodedKey) }
    }


    fun getKey(): ByteArray? {
        val encodedKey = sharedPreferences.getString(CLIENT_SIDE_ENCRYPTION_MASTER_KEY_PREF_KEY, null)
        return encodedKey?.let {
            android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
        }
    }

    fun hasKey(): Boolean {
        return sharedPreferences.contains(CLIENT_SIDE_ENCRYPTION_MASTER_KEY_PREF_KEY)
    }

    fun clearKey() {
        sharedPreferences.edit { remove(CLIENT_SIDE_ENCRYPTION_MASTER_KEY_PREF_KEY) }
    }
}
