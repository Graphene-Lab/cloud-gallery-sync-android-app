package com.cloud.sync.domain.repositroy

import com.cloud.sync.data.network.payment.SubscriptionPlan
import com.cloud.sync.domain.model.CloudSpaceCredentials

interface ICloudSpaceRepository {
    suspend fun getCloudSpaceCredentials(): Result<CloudSpaceCredentials>
    suspend fun getCurrentSubscriptionPlan(): Result<SubscriptionPlan>
}