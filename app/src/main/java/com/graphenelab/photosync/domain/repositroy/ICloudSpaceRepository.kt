package com.graphenelab.photosync.domain.repositroy

import com.graphenelab.photosync.data.network.payment.SubscriptionPlan
import com.graphenelab.photosync.domain.model.CloudSpaceCredentials

interface ICloudSpaceRepository {
    suspend fun getCloudSpaceCredentials(): Result<CloudSpaceCredentials>
    suspend fun getCurrentSubscriptionPlan(): Result<SubscriptionPlan>
}