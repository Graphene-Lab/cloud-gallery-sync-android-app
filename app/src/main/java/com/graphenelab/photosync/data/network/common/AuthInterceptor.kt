package com.graphenelab.photosync.data.network.common

import com.graphenelab.photosync.domain.repositroy.IOauthTokenRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val oauthTokenRepository: IOauthTokenRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val accessToken = oauthTokenRepository.getAccessToken()

        // Building the new request, adding the Authorization header if a token exists
        val requestBuilder = originalRequest.newBuilder()
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}