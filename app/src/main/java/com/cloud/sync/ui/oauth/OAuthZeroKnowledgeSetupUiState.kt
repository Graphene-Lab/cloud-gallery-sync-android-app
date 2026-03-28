package com.cloud.sync.ui.oauth

enum class FirstTimeZeroKnowledgeChoice {
    GENERATE_PASSPHRASE,
    USE_OWN_PASSPHRASE,
    SKIP
}

data class OAuthZeroKnowledgeSetupUiState(
    val isInitialized: Boolean = false,
    val isFirstKeycloakLogin: Boolean = true,
    val firstTimeChoice: FirstTimeZeroKnowledgeChoice = FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE,
    val useZeroKnowledgeForExistingAccount: Boolean = true,
    val passphraseInput: String = "",
    val generatedPassphrase: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isEncryptionSetupComplete: Boolean = false
)
