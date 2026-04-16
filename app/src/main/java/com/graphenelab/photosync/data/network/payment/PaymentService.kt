package com.graphenelab.photosync.data.network.payment

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PaymentService {
    @GET("subscription/plans")
    suspend fun getSubscriptionPlans(): List<SubscriptionPlan>

    @POST("payment/intent")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): PaymentIntentResponse
}