package com.graphenelab.photosync.ui.mnemonic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemonicScreen(
    mnemonicViewModel: MnemonicViewModel = hiltViewModel(),
    onScreenDisplayed: (() -> Unit)? = null,
    onMnemonicConfirmed: () -> Unit
) {
    val uiState by mnemonicViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        onScreenDisplayed?.invoke()
    }

    LaunchedEffect(uiState.isKeySaved) {
        if (uiState.isKeySaved) onMnemonicConfirmed()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Back button for non-initial modes
            if (uiState.mode != MnemonicMode.CHOOSE_ACTION) {
                IconButton(
                    onClick = { mnemonicViewModel.goBack() },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (uiState.mode) {
                    MnemonicMode.CHOOSE_ACTION -> {
                        ChooseActionView(
                            onGenerateNew = { mnemonicViewModel.selectGenerateNew() },
                            onRecover = { mnemonicViewModel.selectRecover() },
                            onSkip = {
                                mnemonicViewModel.setEncryptionEnabled(false)
                                onMnemonicConfirmed()
                            }
                        )
                    }
                    MnemonicMode.GENERATE_NEW -> {
                        WordCountSelectionView(
                            onGenerate = { mnemonicViewModel.generateMnemonic(it) }
                        )
                    }
                    MnemonicMode.DISPLAY_GENERATED -> {
                        if (uiState.isLoading) {
                            CircularProgressIndicator()
                        } else {
                            MnemonicDisplay(
                                mnemonic = uiState.mnemonic,
                                onConfirm = { mnemonicViewModel.saveMasterKeyFromMnemonic() }
                            )
                        }
                    }
                    MnemonicMode.RECOVER_INPUT -> {
                        RecoverInputView(
                            recoveryMnemonic = uiState.recoveryMnemonic,
                            isRecovering = uiState.isRecovering,
                            errorMessage = uiState.errorMessage,
                            onMnemonicChange = { mnemonicViewModel.updateRecoveryMnemonic(it) },
                            onRecover = { mnemonicViewModel.recoverFromMnemonic() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseActionView(
    onGenerateNew: () -> Unit,
    onRecover: () -> Unit,
    onSkip: () -> Unit
) {
    Text(
        "Secure Your Account",
        style = MaterialTheme.typography.headlineMedium
    )
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        "Set up client-side encryption to protect your data",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onGenerateNew,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Generate New Passphrase")
        }
        
        OutlinedButton(
            onClick = onRecover,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Recover from Existing one")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                "Skip (Not Recommended)",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun WordCountSelectionView(onGenerate: (Int) -> Unit) {
    Text(
        "Choose Passphrase Length",
        style = MaterialTheme.typography.headlineMedium
    )
    Spacer(modifier = Modifier.height(16.dp))
    
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(28.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = buildAnnotatedString {
                append("This passphrase is your ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("only")
                }
                append(" way to recover your data. Write it down and keep it safe offline. Do NOT share it.")
            },
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { onGenerate(12) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Generate 12-Word Passphrase")
        }
        Button(
            onClick = { onGenerate(24) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Generate 24-Word Passphrase")
        }
    }
}

@Composable
private fun RecoverInputView(
    recoveryMnemonic: String,
    isRecovering: Boolean,
    errorMessage: String?,
    onMnemonicChange: (String) -> Unit,
    onRecover: () -> Unit
) {
    Text(
        "Recover Your Account",
        style = MaterialTheme.typography.headlineMedium
    )
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        "Enter your 12 or 24-word recovery passphrase:",
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(modifier = Modifier.height(16.dp))
    
    OutlinedTextField(
        value = recoveryMnemonic,
        onValueChange = onMnemonicChange,
        label = { Text("Recovery Passphrase") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        isError = errorMessage != null,
        enabled = !isRecovering
    )
    
    errorMessage?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = onRecover,
        enabled = recoveryMnemonic.isNotBlank() && !isRecovering,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isRecovering) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isRecovering) "Recovering..." else "Recover Account")
    }
}

@Composable
private fun MnemonicDisplay(
    mnemonic: String,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val words = mnemonic.split(" ")

    Text(
        "Your Recovery Passphrase",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(16.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            words.forEachIndexed { index, word ->
                Text(
                    text = "${index + 1}. $word",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    OutlinedButton(
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Recovery Passphrase", mnemonic)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Recovery passphrase copied to clipboard!", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("Copy Passphrase")
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("I have saved my passphrase, Continue")
    }
}
