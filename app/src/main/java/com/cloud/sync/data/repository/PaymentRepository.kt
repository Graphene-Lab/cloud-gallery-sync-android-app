package com.cloud.sync.data.repository

import com.cloud.sync.data.network.payment.PaymentIntentRequest
import com.cloud.sync.data.network.payment.PaymentService
import com.cloud.sync.data.network.payment.SubscriptionPlan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentService: PaymentService
) {
    suspend fun getSubscriptionPlans(): List<SubscriptionPlan> {
        return paymentService.getSubscriptionPlans()
    }

    suspend fun createPaymentIntent(planId: String): String {
        val response = paymentService.createPaymentIntent(PaymentIntentRequest(planId))
        return response.clientSecret
    }
}