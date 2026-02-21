package com.cloud.sync.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    qrEncrypted: String?,
    authViewModel: AuthViewModel = hiltViewModel(),
    onAuthenticationSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthenticationSuccess()
        }
    }

    val isQrAvailable = !qrEncrypted.isNullOrBlank()

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
                    text = "Enter Your PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter the 6-digit PIN from the cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isQrAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No QR code provided. Please scan a QR code first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                // PIN Input Field
                PinInputField(
                    pin = uiState.pin,
                    onPinChanged = { authViewModel.onPinChanged(it) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Authenticate Button
                Button(
                    onClick = { authViewModel.authenticate(qrEncrypted) },
                    enabled = isQrAvailable && uiState.pin.length == 6 && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            // Use the color that contrasts with the button's primary color
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = "Authenticate", fontSize = 16.sp)
                    }
                }

                // Display error messages if any
                uiState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PinInputField(
    pin: String,
    onPinChanged: (String) -> Unit,
    pinLength: Int = 6
) {
    BasicTextField(
        value = pin,
        onValueChange = {
            if (it.length <= pinLength && it.all { char -> char.isDigit() }) {
                onPinChanged(it)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pinLength) { index ->
                    val char = when {
                        index < pin.length -> pin[index].toString()
                        else -> ""
                    }
                    val isFocused = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                // Use the primary color when focused, otherwise use a less prominent color
                                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            // Use the text color that corresponds to the surface variant
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    // This is a preview and won't have a real ViewModel
    // we can simulate different states here
    AuthScreen(qrEncrypted = null, onAuthenticationSuccess = {})
}