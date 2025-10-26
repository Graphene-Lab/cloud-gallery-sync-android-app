package com.cloud.sync.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.domain.repositroy.ICloudSpaceRepository
import com.cloud.sync.domain.repositroy.IOauthTokenRepository
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
    private val cloudSpaceRepository: ICloudSpaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadCurrentSubscriptionPlan()
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
            
            try {
                val currentPlan = cloudSpaceRepository.getCurrentSubscriptionPlan()
                _uiState.update { 
                    it.copy(
                        currentPlan = currentPlan,
                        isLoadingPlan = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingPlan = false,
                        planError = e.message ?: "Failed to load subscription plan"
                    )
                }
            }
        }
    }

    fun refreshSubscriptionPlan() {
        loadCurrentSubscriptionPlan()
    }

    fun logout() {
        oauthTokenRepository.clearTokens()
    }
}