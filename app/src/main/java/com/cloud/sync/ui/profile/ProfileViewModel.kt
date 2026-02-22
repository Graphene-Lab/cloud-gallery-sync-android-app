package com.cloud.sync.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.domain.repositroy.ICloudSpaceRepository
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import com.cloud.sync.domain.repositroy.IOauthTokenRepository
import com.cloud.sync.domain.repositroy.ISessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val oauthTokenRepository: IOauthTokenRepository,
    private val cloudSpaceRepository: ICloudSpaceRepository,
    private val cseMasterKeyRepository: ICseMasterKeyRepository,
    private val sessionRepository: ISessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadCurrentSubscriptionPlan()
        loadCloudCredentials()
        checkLoginMode()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val userEmail = oauthTokenRepository.getEmail()
            _uiState.update { it.copy(email = userEmail) }
        }
    }

    private fun loadCurrentSubscriptionPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlan = true, planError = null) }
            cloudSpaceRepository.getCurrentSubscriptionPlan().onSuccess { currentPlan ->
                _uiState.update {
                    it.copy(
                        currentPlan = currentPlan,
                        isLoadingPlan = false
                    )
                }
                return@onSuccess
            }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingPlan = false,
                            planError = e.message ?: "Failed to load subscription plan"
                        )
                    }
                    return@onFailure
                }
        }
    }

    private fun loadCloudCredentials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCredentials = true, credentialsError = null) }
            try {
                val credentials = sessionRepository.loadCloudSpaceCredentials()
                _uiState.update {
                    it.copy(
                        cloudCredentials = credentials,
                        isLoadingCredentials = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCredentials = false,
                        credentialsError = e.message ?: "Failed to load credentials"
                    )
                }
            }
        }
    }

    private fun checkLoginMode() {
        viewModelScope.launch {
            // If user has no email (didn't login via OAuth), they're in QR mode
            val userEmail = oauthTokenRepository.getEmail()
            _uiState.update { it.copy(isQrLoginMode = userEmail.isNullOrBlank()) }
        }
    }

    fun refreshSubscriptionPlan() {
        loadCurrentSubscriptionPlan()
    }

    fun logout() {
        oauthTokenRepository.clearTokens()
        cseMasterKeyRepository.clearKey()
    }
}