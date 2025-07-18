package com.cloud.sync.data.network.user

import retrofit2.http.GET

interface CloudSpaceService {
    @GET("api/user/cloud-space/credentials")
    suspend fun getCloudSpaceCredentials(): CloudSpaceCredentialsResponse
}