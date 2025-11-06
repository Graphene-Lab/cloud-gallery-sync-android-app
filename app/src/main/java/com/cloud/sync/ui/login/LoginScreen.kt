package com.cloud.sync.ui.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloud.sync.BuildConfig

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onLoginAndCseKeyGenerated: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation when authenticated
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Authenticated) {
            Log.d(TAG, "Authentication successful, navigating to main screen")
            if (viewModel.isCseMasterKeyGenerated()) {
                onLoginAndCseKeyGenerated()
            } else {
                onLoginSuccess()
            }
        }
    }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Auth launcher result received, handling in ViewModel")
                }
                viewModel.handleAuthResult(intent)
            }
        } else {
            Log.w(TAG, "Authorization cancelled or failed. Result code: ${result.resultCode}")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is LoginUiState.Loading -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Showing loading state")
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Authentication in Progress. Please Wait...",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is LoginUiState.Authenticated -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Showing authenticated state")
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Successfully authenticated! Connecting...",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is LoginUiState.Unauthenticated -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Showing unauthenticated state")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Welcome to Cloud Sync",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Sign in button clicked, launching auth intent")
                            }
                            val authIntent = viewModel.getAuthIntent()
                            authLauncher.launch(authIntent)
                        }) {
                            Text("Sign In with Keycloak")
                        }
                    }
                }


                is LoginUiState.Error -> {
                    Log.w(TAG, "Showing error state: ${(uiState as LoginUiState.Error).message}")
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(48.dp)
                                )

                                Text(
                                    text = "Oops! Something went wrong",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = (uiState as LoginUiState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "Try again button clicked")
                                }
                                viewModel.retryAuth()
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Try Again",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}