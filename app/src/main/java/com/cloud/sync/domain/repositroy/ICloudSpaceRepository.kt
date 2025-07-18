package com.cloud.sync.domain.repositroy

import com.cloud.sync.domain.model.CloudSpaceCredentials

interface ICloudSpaceRepository {
    suspend fun getCloudSpaceCredentials(): CloudSpaceCredentials
}