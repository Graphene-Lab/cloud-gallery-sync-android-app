package com.cloud.sync.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cloud.sync.domain.model.SessionDataModel
import com.cloud.sync.domain.repositroy.ISessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ISessionRepository {

    companion object {
        private const val PREF_NAME = "encrypted_session_prefs"
        private const val SESSION_KEY = "session_data"
        private const val TAG = "SessionRepository"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun saveSession(session: SessionDataModel) = withContext(Dispatchers.IO) {
        try {
            val sessionJson = json.encodeToString(session)
            encryptedPrefs.edit()
                .putString(SESSION_KEY, sessionJson)
                .apply()
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

            // TODO: fix this issue
            // Handle public key if needed (public key can be recreated from base64)
//            sessionData.publicKeyB64?.let { pubKeyB64 ->
//                try {
//                    val keyFactory = java.security.KeyFactory.getInstance("RSA")
//                    val keyBytes = java.util.Base64.getDecoder().decode(pubKeyB64)
//                    val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
//                    val publicKey = keyFactory.generatePublic(keySpec)
//                    session.publicKey = publicKey
//                } catch (e: Exception) {
//                    Log.e(TAG, "Failed to restore public key from base64", e)
//                }
//            }

            Log.d(TAG, "Communication session loaded successfully")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load communication session", e)
            null
        }
    }
}