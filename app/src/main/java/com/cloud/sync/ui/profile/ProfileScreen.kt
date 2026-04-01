package com.cloud.sync.ui.profile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Remember the URIs we're trying to delete so we can retry after permission granted
    var pendingDeleteUris by remember { mutableStateOf<List<android.net.Uri>?>(null) }

    // Launcher for delete permission request (Android 11+)
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        Log.d("ProfileScreen", "Delete permission result: resultCode=${result.resultCode}, OK=${Activity.RESULT_OK}")
        val granted = result.resultCode == Activity.RESULT_OK

        if (granted && pendingDeleteUris != null) {
            // Android 11+ - createDeleteRequest already deleted the files
            val deletedCount = pendingDeleteUris!!.size
            Log.d("ProfileScreen", "Permission granted, $deletedCount photos deleted (Android 11+)")
            viewModel.onDeletePermissionResult(true, deletedCount)
            pendingDeleteUris = null
        } else {
            Log.d("ProfileScreen", "Permission denied or no pending URIs")
            viewModel.onDeletePermissionResult(false, 0)
            pendingDeleteUris = null
        }
    }

    // Handle photo URIs when they're set - create delete request in UI layer
    LaunchedEffect(uiState.photoUrisToDelete) {
        uiState.photoUrisToDelete?.let { photoUris ->
            if (photoUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Store URIs for tracking
                pendingDeleteUris = photoUris

                // Android 11+ - Use MediaStore.createDeleteRequest (deletes files when granted)
                try {
                    Log.d("ProfileScreen", "Creating delete request for ${photoUris.size} photos (Android 11+)")
                    val urisArrayList = ArrayList(photoUris)
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, urisArrayList)

                    Log.d("ProfileScreen", "Successfully created PendingIntent, launching dialog")
                    deletePermissionLauncher.launch(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    )

                    viewModel.clearPhotoUrisToDelete()
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Failed to create delete request (Android 11+)", e)
                    pendingDeleteUris = null
                    viewModel.onDeletePermissionResult(false, 0)
                }
            }
        }
    }

    fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Information") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Only show Email and Subscription Plan if NOT in QR login mode
            if (!uiState.isQrLoginMode) {
                // Email Section
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.email ?: "Loading...",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Cloud Credentials Section (always shown)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Cloud Credentials",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        uiState.isLoadingCredentials -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading credentials...")
                            }
                        }

                        uiState.credentialsError != null -> {
                            Text(
                                text = "Error: ${uiState.credentialsError}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        uiState.cloudCredentials == null -> {
                            Text(
                                text = "No credentials saved",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        else -> {
                            val credentials = uiState.cloudCredentials
                            Text(
                                text = "Encrypted QR code",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = credentials?.qrEncrypted.orEmpty(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(
                                        "Encrypted QR code",
                                        credentials?.qrEncrypted.orEmpty()
                                    )
                                }
                            ) {
                                Text("Copy QR Code")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "PIN",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = credentials?.pin?.toString()?.padStart(6, '0').orEmpty(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(
                                        "PIN",
                                        credentials?.pin?.toString()?.padStart(6, '0').orEmpty()
                                    )
                                }
                            ) {
                                Text("Copy PIN")
                            }

                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current Plan Section (only show if NOT in QR login mode)
            if (!uiState.isQrLoginMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Plan",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!uiState.isLoadingPlan) {
                                IconButton(onClick = { viewModel.refreshSubscriptionPlan() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when {
                            uiState.isLoadingPlan -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading plan...")
                                }
                            }

                            uiState.planError != null -> {
                                Text(
                                    text = "Error: ${uiState.planError}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            uiState.currentPlan != null -> {
                                val plan = uiState.currentPlan!!
                                Column {
                                    Text(
                                        text = plan.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = plan.displayAmount,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = plan.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button(onClick = onNavigateToSubscription) {
                //     Text("Manage Subscription")
                // }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clear Synced Photos Section - Only show for Android 11+ (API 30+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Clear Synced Photos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Delete already synced photos from your device gallery to free up space.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = !uiState.isDeletingSyncedPhotos,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (uiState.isDeletingSyncedPhotos) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (uiState.isDeletingSyncedPhotos) "Deleting..." else "Clear Synced Photos")
                        }

                        // Show success or error message
                        uiState.deletedPhotosCount?.let { count ->
                            Spacer(modifier = Modifier.height(8.dp))
                            if (count > 0) {
                                Text(
                                    text = "Successfully deleted $count photo(s)",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "No synced photos to delete",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        uiState.deleteError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Error: $error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Confirmation Dialog
                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("Delete Synced Photos?") },
                        text = {
                            Text("This will permanently delete all synced photos from your device gallery. The photos will still be available in the cloud. This action cannot be undone.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    viewModel.deleteSyncedPhotos()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen({}, {}, {})
}