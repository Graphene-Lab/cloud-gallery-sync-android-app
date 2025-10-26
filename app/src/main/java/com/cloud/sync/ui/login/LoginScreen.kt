package com.cloud.sync.ui.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    // This effect will run when the uiState changes to Authenticated
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Authenticated) {
            onLoginSuccess()
        }
    }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                coroutineScope.launch {
                    try {
                        viewModel.exchangeCodeForToken(it)
                        viewModel.connectToCloud()
                        onLoginSuccess()
                    } catch (e: AuthorizationException) {
                        Log.w("LoginScreen", "Authorization failed or was cancelled", e)
                    }
                }
            }
        } else {
            Log.w("LoginScreen", "Authorization flow was cancelled. Result code: ${result.resultCode}")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Show UI based on the state from the ViewModel
            when (uiState) {
                is LoginUiState.Loading, is LoginUiState.Authenticated -> {
                    // Show a loading spinner while checking auth or navigating
                    CircularProgressIndicator()
                }
                is LoginUiState.Unauthenticated -> {
                    // Show the login button only if the user is not authenticated
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Welcome to Cloud Sync", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            val authIntent = viewModel.getAuthIntent()
                            authLauncher.launch(authIntent)
                        }) {
                            Text("Sign In with Keycloak")
                        }
                    }
                }
            }
        }
    }
}