package com.cloud.sync.data.repository

import com.cloud.sync.data.network.payment.SubscriptionPlan
import com.cloud.sync.data.network.user.CloudSpaceService
import com.cloud.sync.domain.model.CloudSpaceCredentials
import com.cloud.sync.domain.repositroy.ICloudSpaceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSpaceRepository @Inject constructor(
    private val cloudSpaceService: CloudSpaceService
) : ICloudSpaceRepository {

    override suspend fun getCloudSpaceCredentials(): CloudSpaceCredentials {
        val response = cloudSpaceService.getCloudSpaceCredentials()
        return CloudSpaceCredentials(
            qrEncrypted = response.qrEncrypted,
            pin = response.pin
        )
    }

    override suspend fun getCurrentSubscriptionPlan(): SubscriptionPlan {
        return cloudSpaceService.getCurrentSubscriptionPlan()
    }
}