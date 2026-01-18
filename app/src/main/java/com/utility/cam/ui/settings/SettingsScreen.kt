package com.utility.cam.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.utility.cam.BuildConfig
import com.utility.cam.data.NotificationHelper
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.TTLDuration
import com.utility.cam.ui.permissions.isNotificationPermissionGranted
import com.utility.cam.worker.PhotoCleanupWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val defaultTTL by preferencesManager.getDefaultTTL().collectAsState(initial = TTLDuration.TWENTY_FOUR_HOURS)
    val notificationsEnabled by preferencesManager.getNotificationsEnabled().collectAsState(initial = true)
    val hasNotificationPermission = isNotificationPermissionGranted()
    val cleanupDelaySeconds by preferencesManager.getCleanupDelaySeconds().collectAsState(initial = 10)

    var cleanupDelayInput by remember { mutableStateOf("") }
    var hasUserEdited by remember { mutableStateOf(false) }

    // Initialize input with current value from preferences
    LaunchedEffect(cleanupDelaySeconds) {
        // Only update if user hasn't edited the field yet
        if (!hasUserEdited) {
            cleanupDelayInput = cleanupDelaySeconds.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Default Expiration Time",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Choose how long photos should be kept before automatically deleting",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TTLDuration.entries
                .filter { !it.isDebugOnly || BuildConfig.DEBUG }
                .forEach { duration ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = defaultTTL == duration,
                            onClick = {
                                coroutineScope.launch {
                                    preferencesManager.setDefaultTTL(duration)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            duration.displayName,
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Notifications",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cleanup Notifications",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Get notified when expired photos are deleted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesManager.setNotificationsEnabled(enabled)
                        }
                    }
                )
            }

            // Show permission button if notifications are enabled but permission not granted
            if (notificationsEnabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Notification Permission Required",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "To receive cleanup notifications, you need to grant notification permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Open app settings
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Open Settings")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))

            // Debug section (only visible in debug builds)
            if (BuildConfig.DEBUG) {
                Text(
                    "Debug Tools",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Testing utilities for development",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cleanup delay input
                OutlinedTextField(
                    value = cleanupDelayInput,
                    onValueChange = { newValue ->
                        // Mark as edited by user
                        hasUserEdited = true

                        // Only allow digits
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            cleanupDelayInput = newValue
                            // Update preference if valid
                            newValue.toIntOrNull()?.let { seconds ->
                                if (seconds > 0 && seconds <= 3600) { // Max 1 hour
                                    coroutineScope.launch {
                                        preferencesManager.setCleanupDelaySeconds(seconds)
                                        // Reset the flag after saving so next restart shows correct value
                                        hasUserEdited = false
                                        Toast.makeText(
                                            context,
                                            "Cleanup delay updated to $seconds seconds. Restart app to apply.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    label = { Text("Cleanup Worker Delay (seconds)") },
                    supportingText = {
                        Text("Time between cleanup worker runs (1-3600 seconds). Requires app restart.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = cleanupDelayInput.toIntOrNull()?.let { it <= 0 || it > 3600 } ?: false
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Trigger immediate cleanup
                        Toast.makeText(context, "Triggering cleanup worker...", Toast.LENGTH_SHORT).show()
                        val cleanupRequest = OneTimeWorkRequestBuilder<PhotoCleanupWorker>().build()
                        WorkManager.getInstance(context).enqueue(cleanupRequest)

                        // Observe the work status
                        WorkManager.getInstance(context)
                            .getWorkInfoByIdLiveData(cleanupRequest.id)
                            .observeForever { workInfo ->
                                when (workInfo?.state) {
                                    WorkInfo.State.SUCCEEDED -> {
                                        Toast.makeText(context, "Cleanup completed!", Toast.LENGTH_SHORT).show()
                                    }
                                    WorkInfo.State.FAILED -> {
                                        Toast.makeText(context, "Cleanup failed!", Toast.LENGTH_SHORT).show()
                                    }
                                    WorkInfo.State.RUNNING -> {
                                        Toast.makeText(context, "Cleanup running...", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {}
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trigger Cleanup Now")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        // Test notification directly
                        Toast.makeText(context, "Sending test notification...", Toast.LENGTH_SHORT).show()
                        NotificationHelper.sendPhotoCleanupNotification(context, 1)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Notification")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        // Check WorkManager status
                        val workManager = WorkManager.getInstance(context)
                        val workInfos = workManager.getWorkInfosForUniqueWork("photo_cleanup").get()

                        if (workInfos.isEmpty()) {
                            Toast.makeText(context, "No periodic worker scheduled!", Toast.LENGTH_LONG).show()
                        } else {
                            val workInfo = workInfos[0]
                            val status = "Worker Status: ${workInfo.state}\n" +
                                        "Run Attempt: ${workInfo.runAttemptCount}\n" +
                                        "Tags: ${workInfo.tags.joinToString()}"
                            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Worker Status")
                }

                Spacer(modifier = Modifier.height(32.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                "About Utility Cam",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "A secondary camera for temporary photos. Everything captured in this app automatically deletes itself after the set time period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "• Photos are stored in private app storage, not your main gallery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "• Tap 'Keep Forever' on any photo to save it permanently",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "• The home screen widget shows your active photos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
