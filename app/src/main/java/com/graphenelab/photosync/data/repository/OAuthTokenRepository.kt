package com.graphenelab.photosync.data.repository

import com.graphenelab.photosync.data.local.secure.TokenStorage
import com.graphenelab.photosync.domain.repositroy.IOauthTokenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OauthTokenRepository @Inject constructor(
    private val tokenStorage: TokenStorage
) : IOauthTokenRepository {

    override fun saveTokens(accessToken: String, refreshToken: String) {
        tokenStorage.saveTokens(accessToken, refreshToken)
    }

    override fun getAccessToken(): String? {
        return tokenStorage.getAccessToken()
    }

    override fun getRefreshToken(): String? {
        return tokenStorage.getRefreshToken()
    }

    override fun clearTokens() {
        tokenStorage.clearTokens()
    }

    override fun getEmail(): String? {
        return tokenStorage.getEmail()
    }
}