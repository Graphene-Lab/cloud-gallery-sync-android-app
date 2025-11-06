package com.cloud.sync.ui.mnemonic

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom
import javax.inject.Inject

@HiltViewModel
class MnemonicViewModel @Inject constructor(
    private val keyRepository: ICseMasterKeyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MnemonicUiState())
    val uiState: StateFlow<MnemonicUiState> = _uiState.asStateFlow()

    fun selectGenerateNew() {
        _uiState.update { it.copy(mode = MnemonicMode.GENERATE_NEW) }
    }

    fun selectRecover() {
        _uiState.update { 
            it.copy(
                mode = MnemonicMode.RECOVER_INPUT,
                errorMessage = null,
                recoveryMnemonic = ""
            ) 
        }
    }

    fun generateMnemonic(wordCount: Int = 12) {
        _uiState.update { it.copy(isLoading = true) }

        val strength = if (wordCount == 24) 256 else 128
        val entropy = ByteArray(strength / 8)
        SecureRandom().nextBytes(entropy)
        val newMnemonic = Mnemonics.MnemonicCode(entropy).joinToString(" ")

        _uiState.update {
            it.copy(
                mnemonic = newMnemonic,
                isLoading = false,
                mode = MnemonicMode.DISPLAY_GENERATED
            )
        }
    }

    fun updateRecoveryMnemonic(mnemonic: String) {
        _uiState.update { 
            it.copy(
                recoveryMnemonic = mnemonic,
                errorMessage = null
            ) 
        }
    }

    fun recoverFromMnemonic() {
        val mnemonic = _uiState.value.recoveryMnemonic.trim()
        
        if (mnemonic.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your mnemonic phrase") }
            return
        }

        _uiState.update { it.copy(isRecovering = true, errorMessage = null) }

        try {
            val mnemonicCode = Mnemonics.MnemonicCode(mnemonic)
            val seed = mnemonicCode.toSeed()
            keyRepository.saveKey(seed)
            
            _uiState.update { 
                it.copy(
                    isKeySaved = true,
                    isRecovering = false
                ) 
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    errorMessage = "Invalid mnemonic phrase. Please check and try again.",
                    isRecovering = false
                ) 
            }
        }
    }

    fun saveMasterKeyFromMnemonic() {
        if (uiState.value.mnemonic.isNotBlank()) {
            val mnemonicCode = Mnemonics.MnemonicCode(uiState.value.mnemonic)
            val seed = mnemonicCode.toSeed()
            keyRepository.saveKey(seed)
            _uiState.update { it.copy(isKeySaved = true) }
        }
    }

    fun goBack() {
        _uiState.update { 
            it.copy(
                mode = MnemonicMode.CHOOSE_ACTION,
                errorMessage = null,
                recoveryMnemonic = "",
                mnemonic = ""
            ) 
        }
    }
}