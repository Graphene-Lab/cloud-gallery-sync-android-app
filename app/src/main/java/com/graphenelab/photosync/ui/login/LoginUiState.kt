package com.graphenelab.photosync.ui.login


sealed interface LoginUiState {
    data object Loading : LoginUiState
    data object Unauthenticated : LoginUiState
    data class Error(val message: String) : LoginUiState
}
