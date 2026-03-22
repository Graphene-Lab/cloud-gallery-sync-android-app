package com.cloud.sync.ui.auth

// Represents the state of the authentication screen
data class AuthUiState(
    val pin: String = "",
    val useZeroKnowledge: Boolean = true,
    val zeroKnowledgePassphrase: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)