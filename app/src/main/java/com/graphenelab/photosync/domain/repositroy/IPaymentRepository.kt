package com.graphenelab.photosync.domain.repositroy

import com.graphenelab.photosync.data.network.payment.SubscriptionPlan

interface IPaymentRepository {
    suspend fun getSubscriptionPlans(): List<SubscriptionPlan>
    suspend fun createPaymentIntent(planId: String): String
}