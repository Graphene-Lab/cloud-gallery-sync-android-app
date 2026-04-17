package com.graphenelab.photosync.ui.profile

import android.net.Uri
import com.graphenelab.photosync.data.network.payment.SubscriptionPlan
import com.graphenelab.photosync.domain.model.CloudSpaceCredentials

data class ProfileUiState(
    val email: String? = null,
    val currentPlan: SubscriptionPlan? = null,
    val isLoadingPlan: Boolean = false,
    val planError: String? = null,
    val cloudCredentials: CloudSpaceCredentials? = null,
    val isLoadingCredentials: Boolean = false,
    val credentialsError: String? = null,
    val isQrLoginMode: Boolean = false,
    val isDeletingSyncedPhotos: Boolean = false,
    val deletedPhotosCount: Int? = null,
    val deleteError: String? = null,
    val photoUrisToDelete: List<Uri>? = null,
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: String? = null
)
