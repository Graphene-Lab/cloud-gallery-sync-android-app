package com.graphenelab.photosync.data.repository

import com.graphenelab.photosync.data.network.payment.PaymentIntentRequest
import com.graphenelab.photosync.data.network.payment.PaymentService
import com.graphenelab.photosync.data.network.payment.SubscriptionPlan
import com.graphenelab.photosync.domain.repositroy.IPaymentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentService: PaymentService
) : IPaymentRepository {
    override suspend fun getSubscriptionPlans(): List<SubscriptionPlan> {
        return paymentService.getSubscriptionPlans()
    }

    override suspend fun createPaymentIntent(planId: String): String {
        val response = paymentService.createPaymentIntent(PaymentIntentRequest(planId))
        return response.clientSecret
    }
}