package com.graphenelab.photosync.ui.login

sealed interface LoginEvent {
    data class OAuthPairingCredentialsResolved(
        val qrEncrypted: String,
        val pin: Int
    ) : LoginEvent
}
