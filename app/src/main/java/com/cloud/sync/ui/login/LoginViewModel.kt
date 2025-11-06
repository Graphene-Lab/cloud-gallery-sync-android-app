package com.cloud.sync.ui.login

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.BuildConfig
import com.cloud.sync.domain.repositroy.ICloudSpaceRepository
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import com.cloud.sync.domain.repositroy.IOauthTokenRepository
import com.cloud.sync.domain.repositroy.ISessionRepository
import com.cloud.sync.manager.DataCenterCloudManager
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
    private val sessionRepository: ISessionRepository,
    private val cseMasterKeyRepository: ICseMasterKeyRepository,
    private val cloudManager: ICloudManager
) : ViewModel() {
    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "init login view model")
        }
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            if (!oauthTokenRepository.getAccessToken()
                    .isNullOrBlank() && sessionRepository.loadSession() != null
            ) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "checkAuthStatus: Authenticated")
                }
                _uiState.value = LoginUiState.Authenticated
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "checkAuthStatus: Unauthenticated")
                }
                _uiState.value = LoginUiState.Unauthenticated
            }
        }
    }

    fun getAuthIntent(): Intent {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getAuthIntent: Creating auth intent")
        }
        return oAuthManager.getAuthIntent()
    }

    // Move the entire auth flow into ViewModel
    fun handleAuthResult(intent: Intent) {
        viewModelScope.launch {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "handleAuthResult: exchanging code for token")
                }
                _uiState.value = LoginUiState.Loading

                // Exchange code for token
                oAuthManager.exchangeCodeForToken(intent)

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "handleAuthResult: getting cloudsSpace credentials & connecting to cloud")
                }

                // Connect to cloud
                cloudSpaceRepository.getCloudSpaceCredentials()
                    .onSuccess { credentials ->
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "fetched cloudSpace credentials: ${credentials.qrEncrypted}")
                        }
                        cloudManager.pair(credentials.qrEncrypted, credentials.pin).onSuccess {
                            Log.d(TAG, "handleAuthResult: Connected to cloud")

                            _uiState.value = LoginUiState.Authenticated
                        }
                            .onFailure { exception ->
                                Log.w(TAG, "handleAuthResult: Failed to connect to cloud", exception)
                                _uiState.value =
                                    LoginUiState.Error(exception.message ?: "Unknown error")
                            }
                    }
                    .onFailure { exception ->
                        Log.w(
                            TAG,
                            "handleAuthResult: Failed to get cloudSpace credentials",
                            exception
                        )
                        _uiState.value =
                            LoginUiState.Error(exception.message ?: "Unknown error")
                    }

            } catch (e: Exception) {
                Log.w(TAG, "handleAuthResult error", e)
                _uiState.value = LoginUiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun retryAuth() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "retryAuth: Retrying authentication")
        }
        _uiState.value = LoginUiState.Unauthenticated
    }

    fun isCseMasterKeyGenerated(): Boolean {
        cseMasterKeyRepository.getKey() ?: return false
        return true
    }
}