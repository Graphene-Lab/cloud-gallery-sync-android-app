package com.cloud.sync.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cloud.sync.domain.model.SessionDataModel
import com.cloud.sync.domain.model.CloudSpaceCredentials
import com.cloud.sync.domain.repositroy.ISessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ISessionRepository {

    companion object {
        private const val PREF_NAME = "encrypted_session_prefs"
        private const val SESSION_KEY = "session_data"
        private const val TAG = "SessionRepository"
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val CLOUD_CREDENTIALS_KEY = "cloud_space_credentials"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        createEncryptedPreferences()
    }

    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Failed to create encrypted preferences due to security exception, clearing corrupted data and retrying", e)
            clearCorruptedPreferences()
            
            // Retry once after clearing corrupted data
            try {
                val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Still failed after clearing corrupted data, falling back to regular SharedPreferences", e2)
                // Fallback to regular SharedPreferences if encryption continues to fail
                context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error creating encrypted preferences, falling back to regular SharedPreferences", e)
            // Fallback to regular SharedPreferences for any other unexpected errors
            context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun clearCorruptedPreferences() {
        try {
            // Clear the encrypted preferences file
            val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$PREF_NAME.xml")
            if (prefsFile.exists()) {
                val deleted = prefsFile.delete()
                Log.d(TAG, "Encrypted preferences file deleted: $deleted")
            }

            // Clear the master key from Android Keystore
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
                Log.d(TAG, "Master key deleted from keystore")
            }

            // Also try to clear any backup files
            val backupFile = File(context.applicationInfo.dataDir, "shared_prefs/$PREF_NAME.xml.bak")
            if (backupFile.exists()) {
                backupFile.delete()
                Log.d(TAG, "Backup preferences file deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing corrupted preferences", e)
        }
    }

    override suspend fun saveSession(session: SessionDataModel) = withContext(Dispatchers.IO) {
        try {
            val sessionJson = json.encodeToString(session)
            encryptedPrefs.edit {
                putString(SESSION_KEY, sessionJson)
            }
            Log.d(TAG, "Session saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session", e)
            throw e
        }
    }

    override suspend fun saveCommunicationSession(session: com.cloud.communication.cryto.Session) = withContext(Dispatchers.IO) {
        val sessionData = SessionDataModel(
            clientId = session.clientId,
            serverId = session.serverId,
            deviceKey = session.deviceKey,
            encryptionType = session.encryptionType,
            symmetricKey = session.symmetricKey?.encoded,
            iv = session.iv,
            pin = session.pin,
            privateKey = session.privateKey?.encoded,
            qrKey = session.qRkey,
            publicKeyB64 = session.publicKeyB64
        )
        saveSession(sessionData)
    }

    override suspend fun loadSession(): SessionDataModel? = withContext(Dispatchers.IO) {
        try {
            val sessionJson = encryptedPrefs.getString(SESSION_KEY, null)
            if (sessionJson != null) {
                Log.d(TAG, "Session loaded successfully")
                json.decodeFromString<SessionDataModel>(sessionJson)
            } else {
                Log.d(TAG, "No saved session found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session", e)
            null
        }
    }

    override suspend fun loadCommunicationSession(): com.cloud.communication.cryto.Session? = withContext(Dispatchers.IO) {
        try {
            val sessionData = loadSession() ?: return@withContext null

            val session = com.cloud.communication.cryto.Session()
            session.clientId = sessionData.clientId
            session.serverId = sessionData.serverId
            session.deviceKey = sessionData.deviceKey
            session.encryptionType = sessionData.encryptionType

            // Handle symmetric key conversion
            sessionData.symmetricKey?.let { keyData ->
                if (sessionData.encryptionType != null) {
                    val secretKey = javax.crypto.spec.SecretKeySpec(keyData, 0, keyData.size, sessionData.encryptionType)
                    session.symmetricKey = secretKey
                }
            }

            session.iv = sessionData.iv
            sessionData.pin?.let { session.pin = it }

            // Handle private key conversion
            sessionData.privateKey?.let { keyData ->
                try {
                    val keyFactory = java.security.KeyFactory.getInstance("RSA")
                    val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyData)
                    val privateKey = keyFactory.generatePrivate(keySpec)
                    session.privateKey = privateKey
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore private key", e)
                }
            }

            session.qRkey = sessionData.qrKey
            session.publicKeyB64 = sessionData.publicKeyB64

            Log.d(TAG, "Communication session loaded successfully")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load communication session", e)
            null
        }
    }

    override suspend fun saveCloudSpaceCredentials(credentials: CloudSpaceCredentials) = withContext(Dispatchers.IO) {
        try {
            val credentialsJson = json.encodeToString(credentials)
            encryptedPrefs.edit {
                putString(CLOUD_CREDENTIALS_KEY, credentialsJson)
            }
            Log.d(TAG, "Cloud space credentials saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cloud space credentials", e)
            throw e
        }
    }

    override suspend fun loadCloudSpaceCredentials(): CloudSpaceCredentials? = withContext(Dispatchers.IO) {
        try {
            val credentialsJson = encryptedPrefs.getString(CLOUD_CREDENTIALS_KEY, null)
            if (credentialsJson != null) {
                Log.d(TAG, "Cloud space credentials loaded successfully")
                json.decodeFromString<CloudSpaceCredentials>(credentialsJson)
            } else {
                Log.d(TAG, "No saved cloud space credentials found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cloud space credentials", e)
            null
        }
    }

    /**
     * Manually clear all session data - useful for logout or when corruption is detected
     */
    suspend fun clearSession() = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit { clear() }
            Log.d(TAG, "Session data cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear session data", e)
        }
    }
}