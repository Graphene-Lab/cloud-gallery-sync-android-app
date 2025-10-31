package com.cloud.sync.domain.repositroy

interface IOauthTokenRepository {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
    fun getEmail(): String?
}
