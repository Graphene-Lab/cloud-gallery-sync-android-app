package com.cloud.sync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.domain.model.CloudSpaceCredentials
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
    private val sessionRepository: ISessionRepository
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

    fun authenticate(qrEncrypted: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val pinValue = uiState.value.pin.toIntOrNull()
                if (qrEncrypted.isNullOrBlank()) {
                    throw IllegalStateException("No QR code provided. Please scan a QR code first.")
                }
                if (pinValue == null) {
                    throw IllegalStateException("Invalid PIN format. Please enter 6 digits.")
                }

                cloudManager.pair(qrEncrypted, pinValue).onSuccess {
                    val credentials = CloudSpaceCredentials(
                        qrEncrypted = qrEncrypted,
                        pin = pinValue
                    )
                    sessionRepository.saveCloudSpaceCredentials(credentials)
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
}