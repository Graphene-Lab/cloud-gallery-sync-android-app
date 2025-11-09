package com.cloud.sync.ui.sync

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    syncViewModel: SyncViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current

    val uiState by syncViewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        Log.d("SyncScreen", "Permission result: $permissions")
        syncViewModel.handlePermissionResult(permissions)


    }

    // One-time launcher setup
    LaunchedEffect(Unit) {
        syncViewModel.setPermissionLauncher(permissionLauncher)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery Sync") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(text = "Status:", style = MaterialTheme.typography.titleMedium)
                        if (!uiState.isFullScanInProgress) {
                            if (uiState.completedPhotos > 0) {
                                Text(
                                    text = "Sync Successful, ${uiState.completedPhotos} photos uploaded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "Ready to Sync",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                        } else {
                            Text(
                                text = "Sync in Progress",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row {
                                Text(
                                    text = "Uploaded Photos:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = uiState.completedPhotos.toString(),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Row {
                                Text(
                                    text = "Failed Photos:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = uiState.failedPhotos.toString(),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Row {
                                Text(
                                    text = "Discovered Photos:",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = uiState.totalPhotosToBeUploaded.toString(),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            Column {
                                Text(text = "Progress Info:")
                                Text(text = uiState.progress?.let { progress ->
                                    "${progress.filename} ${progress.currentChunk}/${progress.totalChunks}"
                                } ?: "waiting...")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Sync New Photos Automatically",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Runs periodically in the background",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = uiState.isBackgroundSyncScheduled,
                        onCheckedChange = syncViewModel::onFromNowSyncToggled,
                        enabled = !uiState.isFullScanInProgress
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                Text("Manual Full Scan", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Finds and uploads any photos missed in the past.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                val isFullScanning = uiState.isFullScanInProgress
                Button(
                    onClick = {
                        if (isFullScanning) syncViewModel.stopFullScan() else syncViewModel.onStartFullScanButtonClicked(
                            context
                        )
                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFullScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                ) {
                    if (isFullScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = LocalContentColor.current,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Stop Full Scan")
                    } else {
                        Text("Start Full Scan")
                    }

                }

                if(uiState.permissionDenied) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
                    Text(
                        text = " You denied granting permissions. Please go to settings to grant them.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}