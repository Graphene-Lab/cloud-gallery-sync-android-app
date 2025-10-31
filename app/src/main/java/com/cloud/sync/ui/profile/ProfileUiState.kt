package com.cloud.sync.ui.profile

import com.cloud.sync.data.network.payment.SubscriptionPlan

data class ProfileUiState(
    val email: String? = null,
    val currentPlan: SubscriptionPlan? = null,
    val isLoadingPlan: Boolean = false,
    val planError: String? = null
)