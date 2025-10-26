package com.cloud.sync.ui.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.domain.repositroy.ICloudSpaceRepository
import com.cloud.sync.domain.repositroy.IOauthTokenRepository
import com.cloud.sync.manager.OAuthManager
import com.cloud.sync.manager.interfaces.ICloudManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val oAuthManager: OAuthManager,
    private val oauthTokenRepository: IOauthTokenRepository,
    private val cloudSpaceRepository: ICloudSpaceRepository,
    private val cloudManager: ICloudManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            if (!oauthTokenRepository.getAccessToken().isNullOrBlank()) {
                _uiState.value = LoginUiState.Authenticated
            } else {
                _uiState.value = LoginUiState.Unauthenticated
            }
        }
    }

    fun getAuthIntent(): Intent {
        return oAuthManager.getAuthIntent()
    }

    suspend fun exchangeCodeForToken(intent: Intent) {
        return oAuthManager.exchangeCodeForToken(intent)
    }

    suspend fun connectToCloud(): Result<Unit> {
        val credentials = cloudSpaceRepository.getCloudSpaceCredentials()
        return cloudManager.pair(credentials.qrEncrypted, credentials.pin)
    }
}