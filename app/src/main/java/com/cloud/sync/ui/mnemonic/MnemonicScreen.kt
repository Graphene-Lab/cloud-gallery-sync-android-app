package com.cloud.sync.ui.mnemonic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MnemonicScreen(
    mnemonicViewModel: MnemonicViewModel = hiltViewModel(),
    onMnemonicConfirmed: () -> Unit
) {
    val uiState by mnemonicViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isKeySaved) {
       if(uiState.isKeySaved  ) onMnemonicConfirmed()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.mnemonic.isNotBlank()) {
                MnemonicDisplay(uiState.mnemonic)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { mnemonicViewModel.saveMasterKeyFromMnemonic() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("I have saved my phrase, Continue")
                }
            } else {
                InitialMnemonicView(onGenerate = { mnemonicViewModel.generateMnemonic(it) })
            }
        }
    }
}

@Composable
private fun InitialMnemonicView(onGenerate: (Int) -> Unit) {
    Text(
        "Secure Your Account",
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
                append("Warning: This phrase is your ")
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
            Text("Generate 12-Word Phrase")
        }
        Button(
            onClick = { onGenerate(24) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Generate 24-Word Phrase")
        }
    }
}

@Composable
private fun MnemonicDisplay(mnemonic: String) {
    val context = LocalContext.current
    val words = mnemonic.split(" ")

    Text(
        "Your Mnemonic Phrase",
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
            val clip = ClipData.newPlainText("Mnemonic Phrase", mnemonic)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Mnemonic copied to clipboard!", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("Copy Phrase")
    }
}