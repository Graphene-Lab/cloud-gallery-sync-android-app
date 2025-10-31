package com.cloud.sync.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.auth0.android.jwt.JWT
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TokenStorage @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String) {
        with(sharedPreferences.edit()) {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            apply()
        }
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString("access_token", null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString("refresh_token", null)
    }

    fun clearTokens() {
        with(sharedPreferences.edit()) {
            remove("access_token")
            remove("refresh_token")
            apply()
        }
    }

    fun getEmail(): String? {
        val token = getAccessToken()
        return if (token != null) {
            try {
                val jwt = JWT(token)
                jwt.getClaim("email").asString()
            } catch (e: Exception) {
                // Handle potential decoding errors
                null
            }
        } else {
            null
        }
    }
}