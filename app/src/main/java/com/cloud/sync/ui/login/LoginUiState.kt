package com.cloud.sync.ui.login


sealed interface LoginUiState {
    data object Loading : LoginUiState
    data object Unauthenticated : LoginUiState
    data class Error(val message: String) : LoginUiState
}
