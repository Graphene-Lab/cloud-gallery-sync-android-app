package com.cloud.sync.ui.oauth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import com.cloud.sync.common.crypto.ZeroKnowledgeAuthUtils
import com.cloud.sync.domain.model.CloudSpaceCredentials
import com.cloud.sync.domain.repositroy.IAppSettingsRepository
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import com.cloud.sync.domain.repositroy.ISessionRepository
import com.cloud.sync.manager.interfaces.ICloudManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject

@HiltViewModel
class OAuthZeroKnowledgeSetupViewModel @Inject constructor(
    private val cloudManager: ICloudManager,
    private val sessionRepository: ISessionRepository,
    private val cseMasterKeyRepository: ICseMasterKeyRepository,
    private val appSettingsRepository: IAppSettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "OAuthZkSetupVM"
    }

    private val _uiState = MutableStateFlow(OAuthZeroKnowledgeSetupUiState())
    val uiState: StateFlow<OAuthZeroKnowledgeSetupUiState> = _uiState.asStateFlow()

    private var pendingCredentials: CloudSpaceCredentials? = null

    fun initialize(qrEncrypted: String?, pin: Int?) {
        if (_uiState.value.isInitialized) return

        if (qrEncrypted.isNullOrBlank() || pin == null) {
            _uiState.update {
                it.copy(
                    isInitialized = true,
                    errorMessage = "Missing cloud credentials. Please sign in again."
                )
            }
            return
        }

        pendingCredentials = CloudSpaceCredentials(qrEncrypted = qrEncrypted, pin = pin)
        _uiState.update {
            it.copy(
                isInitialized = true,
                generatedPassphrase = if (
                    it.isFirstKeycloakLogin &&
                    it.firstTimeChoice == FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE &&
                    it.generatedPassphrase.isBlank()
                ) {
                    createGeneratedPassphrase()
                } else {
                    it.generatedPassphrase
                }
            )
        }
    }

    fun onFirstKeycloakLoginChanged(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                isFirstKeycloakLogin = enabled,
                errorMessage = null,
                isSubmitting = false,
                passphraseInput = "",
                generatedPassphrase = if (enabled) {
                    if (
                        state.firstTimeChoice == FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE &&
                        state.generatedPassphrase.isBlank()
                    ) {
                        createGeneratedPassphrase()
                    } else {
                        state.generatedPassphrase
                    }
                } else {
                    ""
                }
            )
        }
    }

    fun onFirstTimeChoiceChanged(choice: FirstTimeZeroKnowledgeChoice) {
        _uiState.update { state ->
            state.copy(
                firstTimeChoice = choice,
                errorMessage = null,
                isSubmitting = false,
                passphraseInput = if (choice == FirstTimeZeroKnowledgeChoice.USE_OWN_PASSPHRASE) {
                    state.passphraseInput
                } else {
                    ""
                },
                generatedPassphrase = if (
                    state.isFirstKeycloakLogin &&
                    choice == FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE &&
                    state.generatedPassphrase.isBlank()
                ) {
                    createGeneratedPassphrase()
                } else {
                    state.generatedPassphrase
                }
            )
        }
    }

    fun onUseZeroKnowledgeForExistingAccountChanged(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                useZeroKnowledgeForExistingAccount = enabled,
                errorMessage = null,
                isSubmitting = false,
                passphraseInput = if (enabled) state.passphraseInput else ""
            )
        }
    }

    fun onPassphraseChanged(passphrase: String) {
        _uiState.update { state ->
            state.copy(
                passphraseInput = passphrase,
                errorMessage = null,
                isSubmitting = false
            )
        }
    }

    fun generateFirstTimePassphrase(wordCount: Int = 24) {
        val generatedPassphrase = createGeneratedPassphrase(wordCount)
        _uiState.update { state ->
            state.copy(
                firstTimeChoice = FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE,
                generatedPassphrase = generatedPassphrase,
                errorMessage = null,
                isSubmitting = false
            )
        }
    }

    fun completeOAuthPairing() {
        val credentials = pendingCredentials
        if (credentials == null) {
            _uiState.update {
                it.copy(errorMessage = "Cloud credentials are missing. Please sign in again.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val currentState = _uiState.value
                val zeroKnowledgeMasterKey = resolveZeroKnowledgeMasterKey(currentState)
                val zeroKnowledgeChecksum = zeroKnowledgeMasterKey?.let {
                    ZeroKnowledgeAuthUtils.deriveAuthenticationChecksum(it)
                }

                val pairResult = cloudManager.pair(
                    encryptedQrCode = credentials.qrEncrypted,
                    pin = credentials.pin,
                    zeroKnowledgeChecksum = zeroKnowledgeChecksum
                )
                pairResult.getOrElse { throw it }

                sessionRepository.saveCloudSpaceCredentials(credentials)
                if (zeroKnowledgeMasterKey != null) {
                    cseMasterKeyRepository.saveKey(zeroKnowledgeMasterKey)
                }
                appSettingsRepository.setEncryptionEnabled(zeroKnowledgeMasterKey != null)

                val encryptionSetupComplete = resolveEncryptionSetupComplete()
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isAuthenticated = true,
                        isEncryptionSetupComplete = encryptionSetupComplete
                    )
                }
            } catch (error: Exception) {
                Log.w(TAG, "completeOAuthPairing failed", error)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Authentication failed"
                    )
                }
            }
        }
    }

    private fun resolveZeroKnowledgeMasterKey(state: OAuthZeroKnowledgeSetupUiState): ByteArray? {
        return if (state.isFirstKeycloakLogin) {
            when (state.firstTimeChoice) {
                FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE -> {
                    val generatedPassphrase = state.generatedPassphrase.trim()
                    if (generatedPassphrase.isBlank()) {
                        throw IllegalStateException("Generate passphrase before continuing.")
                    }
                    deriveMasterKey(generatedPassphrase)
                }

                FirstTimeZeroKnowledgeChoice.USE_OWN_PASSPHRASE -> {
                    val passphrase = state.passphraseInput.trim()
                    if (passphrase.isBlank()) {
                        throw IllegalStateException("Enter your passphrase.")
                    }
                    deriveMasterKey(passphrase)
                }

                FirstTimeZeroKnowledgeChoice.SKIP -> null
            }
        } else {
            if (!state.useZeroKnowledgeForExistingAccount) {
                null
            } else {
                val passphrase = state.passphraseInput.trim()
                if (passphrase.isBlank()) {
                    throw IllegalStateException("Enter the existing zero-knowledge passphrase.")
                }
                deriveMasterKey(passphrase)
            }
        }
    }

    private fun deriveMasterKey(passphrase: String): ByteArray {
        return runCatching {
            ZeroKnowledgeAuthUtils.deriveMasterKey(passphrase)
        }.getOrElse { cause ->
            throw IllegalStateException(
                "Invalid zero-knowledge passphrase. Please check and try again.",
                cause
            )
        }
    }

    private fun createGeneratedPassphrase(wordCount: Int = 24): String {
        val normalizedWordCount = if (wordCount == 24) 24 else 12
        val strength = if (normalizedWordCount == 24) 256 else 128
        val entropy = ByteArray(strength / 8)
        SecureRandom().nextBytes(entropy)
        return Mnemonics.MnemonicCode(entropy).joinToString(" ")
    }

    private suspend fun resolveEncryptionSetupComplete(): Boolean = withContext(Dispatchers.IO) {
        !appSettingsRepository.isEncryptionEnabled() || cseMasterKeyRepository.hasKey()
    }
}
