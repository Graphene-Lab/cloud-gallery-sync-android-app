package com.cloud.sync.domain.repositroy

import com.cloud.sync.data.network.payment.SubscriptionPlan

interface IPaymentRepository {
    suspend fun getSubscriptionPlans(): List<SubscriptionPlan>
    suspend fun createPaymentIntent(planId: String): String
}