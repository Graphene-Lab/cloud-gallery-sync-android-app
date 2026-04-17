package com.graphenelab.photosync.ui.profile

sealed interface ProfileEvent {
    data class NavigateToLogin(val toastMessage: String? = null) : ProfileEvent
}
