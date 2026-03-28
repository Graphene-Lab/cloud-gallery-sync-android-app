package com.cloud.sync.ui.login


sealed interface LoginUiState {
    data object Loading : LoginUiState
    data class OAuthCredentialsReady(
        val qrEncrypted: String,
        val pin: Int
    ) : LoginUiState
    data object Unauthenticated : LoginUiState
    data class Error(val message: String) : LoginUiState
}
