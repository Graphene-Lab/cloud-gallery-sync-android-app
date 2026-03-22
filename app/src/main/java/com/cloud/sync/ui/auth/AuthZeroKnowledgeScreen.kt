package com.cloud.sync.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthZeroKnowledgeScreen(
    qrEncrypted: String?,
    pin: String,
    authViewModel: AuthViewModel = hiltViewModel(),
    onAuthenticationSuccess: () -> Unit,
    onAuthenticationSuccessWithoutCse: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val isQrAvailable = !qrEncrypted.isNullOrBlank()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            if (authViewModel.isEncryptionSetupComplete()) {
                onAuthenticationSuccess()
            } else {
                onAuthenticationSuccessWithoutCse()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Zero-Knowledge",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Was this account created with zero-knowledge encryption enabled?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.useZeroKnowledge) {
                        Button(
                            onClick = { authViewModel.onUseZeroKnowledgeChanged(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Yes")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { authViewModel.onUseZeroKnowledgeChanged(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Yes")
                        }
                    }

                    if (uiState.useZeroKnowledge) {
                        OutlinedButton(
                            onClick = { authViewModel.onUseZeroKnowledgeChanged(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "No")
                        }
                    } else {
                        Button(
                            onClick = { authViewModel.onUseZeroKnowledgeChanged(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "No")
                        }
                    }
                }

                if (uiState.useZeroKnowledge) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.zeroKnowledgePassphrase,
                        onValueChange = { authViewModel.onZeroKnowledgePassphraseChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Zero-knowledge passphrase") },
                        placeholder = { Text("Enter the existing 12 or 24-word passphrase") },
                        supportingText = {
                            Text("Entering wrong passphrase will lead to authentication failure. Make sure to enter the exact passphrase used during account creation.")
                        },
                        minLines = 3
                    )
                }

                if (!isQrAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No QR code provided. Please scan a QR code first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                uiState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { authViewModel.authenticate(qrEncrypted, pin) },
                        enabled = isQrAvailable &&
                            pin.length == 6 &&
                            !uiState.isLoading &&
                            (!uiState.useZeroKnowledge || uiState.zeroKnowledgePassphrase.isNotBlank()),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = "Authenticate", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
