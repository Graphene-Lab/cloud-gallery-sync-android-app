package com.graphenelab.photosync.ui.oauth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OAuthZeroKnowledgeSetupScreen(
    qrEncrypted: String?,
    pin: Int?,
    onAuthenticationSuccess: () -> Unit,
    onAuthenticationSuccessWithoutCse: () -> Unit,
    onCancel: () -> Unit,
    viewModel: OAuthZeroKnowledgeSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = !uiState.isSubmitting) {
        onCancel()
    }

    LaunchedEffect(qrEncrypted, pin) {
        viewModel.initialize(qrEncrypted, pin)
    }

    LaunchedEffect(uiState.isAuthenticated, uiState.isEncryptionSetupComplete) {
        if (!uiState.isAuthenticated) return@LaunchedEffect
        if (uiState.isEncryptionSetupComplete) {
            onAuthenticationSuccess()
        } else {
            onAuthenticationSuccessWithoutCse()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!uiState.isInitialized) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            OAuthZeroKnowledgeSetupContent(
                setupState = uiState,
                onFirstKeycloakLoginChanged = viewModel::onFirstKeycloakLoginChanged,
                onFirstTimeChoiceChanged = viewModel::onFirstTimeChoiceChanged,
                onUseZeroKnowledgeForExistingAccountChanged = viewModel::onUseZeroKnowledgeForExistingAccountChanged,
                onPassphraseChanged = viewModel::onPassphraseChanged,
                onGeneratePassphrase = viewModel::generateFirstTimePassphrase,
                onDismissError = viewModel::dismissError,
                onContinue = viewModel::completeOAuthPairing,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun OAuthZeroKnowledgeSetupContent(
    setupState: OAuthZeroKnowledgeSetupUiState,
    onFirstKeycloakLoginChanged: (Boolean) -> Unit,
    onFirstTimeChoiceChanged: (FirstTimeZeroKnowledgeChoice) -> Unit,
    onUseZeroKnowledgeForExistingAccountChanged: (Boolean) -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onGeneratePassphrase: () -> Unit,
    onDismissError: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showSecurityInfo by rememberSaveable { mutableStateOf(false) }
    var showPassphraseWarningInfo by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Zero-Knowledge Setup",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Finalize your account pairing by enabling Zero-Knowledge Encryption.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSecurityInfo = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Security details"
                        )
                    }
                }

                if (showSecurityInfo) {
                    AlertDialog(
                        onDismissRequest = { showSecurityInfo = false },
                        title = { Text("How Zero-Knowledge Works") },
                        text = {
                            Text(
                                "Your files are encrypted locally on your device before they are sent to our servers," +
                                    " ensuring only you can access your data; our servers never see it.\n\n" +
                                    "Your passphrase derives a master key. From that master key, " +
                                    "the app derives a unique encryption key for each file using " +
                                    "the file name and file timestamp. This means files are not " +
                                    "encrypted with one static key, which provides stronger isolation."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showSecurityInfo = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This is my first Sign-In",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = setupState.isFirstKeycloakLogin,
                        onCheckedChange = onFirstKeycloakLoginChanged
                    )
                }

                if (setupState.isFirstKeycloakLogin) {
                    Text(
                        text = "Choose how to configure passphrase:",
                        style = MaterialTheme.typography.titleMedium
                    )

                    FirstTimeChoiceButtons(
                        selectedChoice = setupState.firstTimeChoice,
                        onChoiceSelected = onFirstTimeChoiceChanged
                    )

                    when (setupState.firstTimeChoice) {
                        FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Generated passphrase (24 words):",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalIconButton(
                                        onClick = onGeneratePassphrase,
                                        enabled = !setupState.isSubmitting
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Regenerate passphrase"
                                        )
                                    }
                                    FilledTonalIconButton(
                                        onClick = {
                                            clipboardManager.setText(
                                                AnnotatedString(setupState.generatedPassphrase)
                                            )
                                            Toast.makeText(
                                                context,
                                                "Passphrase copied",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        enabled = !setupState.isSubmitting &&
                                            setupState.generatedPassphrase.isNotBlank()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy passphrase"
                                        )
                                    }
                                }
                            }

                            if (setupState.generatedPassphrase.isNotBlank()) {
                                SelectionContainer {
                                    Text(
                                        text = setupState.generatedPassphrase,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Important: Save this passphrase safely.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { showPassphraseWarningInfo = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Passphrase warning details",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                if (showPassphraseWarningInfo) {
                                    AlertDialog(
                                        onDismissRequest = { showPassphraseWarningInfo = false },
                                        title = { Text("Passphrase Warning") },
                                        text = {
                                            Text(
                                                "Do not lose this passphrase. Save it in a secure place. " +
                                                    "If you lose it, you can lose access to your encrypted files. " +
                                                    "This passphrase is not stored on our servers."
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showPassphraseWarningInfo = false }) {
                                                Text("OK")
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        FirstTimeZeroKnowledgeChoice.USE_OWN_PASSPHRASE -> {
                            OutlinedTextField(
                                value = setupState.passphraseInput,
                                onValueChange = onPassphraseChanged,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                label = { Text("Your passphrase") },
                                placeholder = { Text("Enter 12 or 24-word passphrase") },
                                enabled = !setupState.isSubmitting
                            )
                        }

                        FirstTimeZeroKnowledgeChoice.SKIP -> {
                            Text(
                                text = "Encryption will be disabled for this session. This is not recommended.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Was this account originally created with zero-knowledge encryption enabled?",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val useZk = setupState.useZeroKnowledgeForExistingAccount
                        if (useZk) {
                            Button(
                                onClick = { onUseZeroKnowledgeForExistingAccountChanged(true) },
                                modifier = Modifier.weight(1f),
                                enabled = !setupState.isSubmitting
                            ) {
                                Text("Yes")
                            }
                            OutlinedButton(
                                onClick = { onUseZeroKnowledgeForExistingAccountChanged(false) },
                                modifier = Modifier.weight(1f),
                                enabled = !setupState.isSubmitting
                            ) {
                                Text("No")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onUseZeroKnowledgeForExistingAccountChanged(true) },
                                modifier = Modifier.weight(1f),
                                enabled = !setupState.isSubmitting
                            ) {
                                Text("Yes")
                            }
                            Button(
                                onClick = { onUseZeroKnowledgeForExistingAccountChanged(false) },
                                modifier = Modifier.weight(1f),
                                enabled = !setupState.isSubmitting
                            ) {
                                Text("No")
                            }
                        }
                    }

                    if (setupState.useZeroKnowledgeForExistingAccount) {
                        OutlinedTextField(
                            value = setupState.passphraseInput,
                            onValueChange = onPassphraseChanged,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            label = { Text("Existing passphrase") },
                            placeholder = { Text("Enter the same passphrase used at account creation") },
                            enabled = !setupState.isSubmitting
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = !setupState.isSubmitting
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        enabled = !setupState.isSubmitting && canContinue(setupState)
                    ) {
                        if (setupState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Continue")
                        }
                    }
                }

                setupState.errorMessage?.let { message ->
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text("Pairing Failed") },
                        text = { Text(message) },
                        confirmButton = {
                            TextButton(onClick = onDismissError) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstTimeChoiceButtons(
    selectedChoice: FirstTimeZeroKnowledgeChoice,
    onChoiceSelected: (FirstTimeZeroKnowledgeChoice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceButton(
                label = "Generate",
                selected = selectedChoice == FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE,
                onClick = { onChoiceSelected(FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE) },
                modifier = Modifier.weight(1f)
            )
            ChoiceButton(
                label = "Use Mine",
                selected = selectedChoice == FirstTimeZeroKnowledgeChoice.USE_OWN_PASSPHRASE,
                onClick = { onChoiceSelected(FirstTimeZeroKnowledgeChoice.USE_OWN_PASSPHRASE) },
                modifier = Modifier.weight(1f)
            )
        }
        ChoiceButton(
            label = "Skip (Not Recommended)",
            selected = selectedChoice == FirstTimeZeroKnowledgeChoice.SKIP,
            onClick = { onChoiceSelected(FirstTimeZeroKnowledgeChoice.SKIP) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun canContinue(state: OAuthZeroKnowledgeSetupUiState): Boolean {
    return if (state.isFirstKeycloakLogin) {
        when (state.firstTimeChoice) {
            FirstTimeZeroKnowledgeChoice.GENERATE_PASSPHRASE -> state.generatedPassphrase.isNotBlank()
            FirstTimeZeroKnowledgeChoice.USE_OWN_PASSPHRASE -> state.passphraseInput.isNotBlank()
            FirstTimeZeroKnowledgeChoice.SKIP -> true
        }
    } else {
        !state.useZeroKnowledgeForExistingAccount || state.passphraseInput.isNotBlank()
    }
}
