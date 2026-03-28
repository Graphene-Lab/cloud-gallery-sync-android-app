package com.cloud.sync.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun LoginRoute(
    onOAuthPairingCredentialsResolved: (qrEncrypted: String, pin: Int) -> Unit,
    onNavigateToScan: () -> Unit,
    onScreenDisplayed: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.OAuthPairingCredentialsResolved -> {
                    onOAuthPairingCredentialsResolved(event.qrEncrypted, event.pin)
                }
            }
        }
    }

    LoginScreen(
        onNavigateToScan = onNavigateToScan,
        onScreenDisplayed = onScreenDisplayed,
        viewModel = viewModel
    )
}
