package com.cloud.sync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.common.crypto.ZeroKnowledgeAuthUtils
import com.cloud.sync.domain.model.CloudSpaceCredentials
import com.cloud.sync.domain.repositroy.IAppSettingsRepository
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import com.cloud.sync.domain.repositroy.ISessionRepository
import com.cloud.sync.manager.interfaces.ICloudManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val cloudManager: ICloudManager,
    private val sessionRepository: ISessionRepository,
    private val cseMasterKeyRepository: ICseMasterKeyRepository,
    private val appSettingsRepository: IAppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Called when the user enters or changes the PIN.
     * It updates the 'pin' in the UI state.
     */
    fun onPinChanged(pin: String) {
        _uiState.update { currentState ->
            currentState.copy(pin = pin, errorMessage = null)
        }
    }

    fun onUseZeroKnowledgeChanged(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                useZeroKnowledge = enabled,
                zeroKnowledgePassphrase = if (enabled) {
                    currentState.zeroKnowledgePassphrase
                } else {
                    ""
                },
                errorMessage = null
            )
        }
    }

    fun onZeroKnowledgePassphraseChanged(passphrase: String) {
        _uiState.update { currentState ->
            currentState.copy(
                zeroKnowledgePassphrase = passphrase,
                errorMessage = null
            )
        }
    }

    fun authenticate(qrEncrypted: String?, pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val pinValue = pin.toIntOrNull()
                if (qrEncrypted.isNullOrBlank()) {
                    throw IllegalStateException("No QR code provided. Please scan a QR code first.")
                }
                if (pinValue == null) {
                    throw IllegalStateException("Invalid PIN format. Please enter 6 digits.")
                }

                val zeroKnowledgeMasterKey = if (uiState.value.useZeroKnowledge) {
                    runCatching {
                        ZeroKnowledgeAuthUtils.deriveMasterKey(uiState.value.zeroKnowledgePassphrase)
                    }.getOrElse {
                        throw IllegalStateException(
                            "Invalid zero-knowledge passphrase. Please check and try again.",
                            it
                        )
                    }
                } else {
                    null
                }
                val zeroKnowledgeChecksum = zeroKnowledgeMasterKey?.let {
                    ZeroKnowledgeAuthUtils.deriveAuthenticationChecksum(it)
                }

                cloudManager.pair(qrEncrypted, pinValue, zeroKnowledgeChecksum).onSuccess {
                    val credentials = CloudSpaceCredentials(
                        qrEncrypted = qrEncrypted,
                        pin = pinValue
                    )
                    sessionRepository.saveCloudSpaceCredentials(credentials)
                    if (zeroKnowledgeMasterKey != null) {
                        cseMasterKeyRepository.saveKey(zeroKnowledgeMasterKey)
                    }
                    appSettingsRepository.setEncryptionEnabled(zeroKnowledgeMasterKey != null)
                    _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                }.onFailure { exception ->
                    throw exception
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unknown error occurred."
                    )
                }
            }
        }
    }

    /**
     * Checks if the user has skipped client-side encryption setup.
     * @return true if user disabled encryption, false otherwise
     */
    private fun isEncryptionSkipped(): Boolean {
        return !appSettingsRepository.isEncryptionEnabled()
    }

    /**
     * Checks if client-side encryption master key has been generated.
     * @return true if a master key exists in secure storage
     */
    private fun isCseMasterKeyGenerated(): Boolean {
        return cseMasterKeyRepository.hasKey()
    }

    /**
     * Checks if the encryption setup is complete.
     * This is true if either:
     * 1. User has skipped encryption setup, OR
     * 2. User has generated/recovered a master key
     *
     * @return true if encryption setup is complete (can skip mnemonic screen)
     */
    fun isEncryptionSetupComplete(): Boolean {
        return isEncryptionSkipped() || isCseMasterKeyGenerated()
    }
}
