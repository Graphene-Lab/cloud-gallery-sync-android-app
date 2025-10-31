package com.cloud.sync.data.network.user

import com.cloud.sync.data.network.payment.SubscriptionPlan
import retrofit2.http.GET

interface CloudSpaceService {
    @GET("me/cloud-space/credentials")
    suspend fun getCloudSpaceCredentials(): CloudSpaceCredentialsResponse
    
    @GET("me/subscription-plan")
    suspend fun getCurrentSubscriptionPlan(): SubscriptionPlan
}