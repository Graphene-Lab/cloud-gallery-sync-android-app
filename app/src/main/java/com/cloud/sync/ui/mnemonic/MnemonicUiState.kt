package com.cloud.sync.ui.mnemonic

data class MnemonicUiState(
    val mnemonic: String = "",
    val isLoading: Boolean = false,
    val isKeySaved: Boolean = false,
    val mode: MnemonicMode = MnemonicMode.CHOOSE_ACTION,
    val errorMessage: String? = null,
    val recoveryMnemonic: String = "",
    val isRecovering: Boolean = false,
    val isEncryptionEnabled: Boolean = true
)

enum class MnemonicMode {
    CHOOSE_ACTION,
    GENERATE_NEW,
    DISPLAY_GENERATED,
    RECOVER_INPUT
}