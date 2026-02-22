package com.cloud.sync.ui.profile

import com.cloud.sync.data.network.payment.SubscriptionPlan
import com.cloud.sync.domain.model.CloudSpaceCredentials

data class ProfileUiState(
    val email: String? = null,
    val currentPlan: SubscriptionPlan? = null,
    val isLoadingPlan: Boolean = false,
    val planError: String? = null,
    val cloudCredentials: CloudSpaceCredentials? = null,
    val isLoadingCredentials: Boolean = false,
    val credentialsError: String? = null,
    val isQrLoginMode: Boolean = false
)