package com.graphenelab.photosync.ui.login

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graphenelab.photosync.BuildConfig
import com.graphenelab.photosync.domain.repositroy.ICloudSpaceRepository
import com.graphenelab.photosync.manager.OAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val oAuthManager: OAuthManager,
    private val cloudSpaceRepository: ICloudSpaceRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Unauthenticated)
    val uiState = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "init login view model")
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
                        _events.emit(
                            LoginEvent.OAuthPairingCredentialsResolved(
                                qrEncrypted = credentials.qrEncrypted,
                                pin = credentials.pin
                            )
                        )
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
}
