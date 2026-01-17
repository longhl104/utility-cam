package com.utility.cam.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.utility.cam.data.PreferencesManager
import kotlinx.coroutines.launch

/**
 * Handles notification permission request on first launch
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionHandler(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val isFirstLaunch by preferencesManager.isFirstLaunch().collectAsState(initial = false)
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Only request notification permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        ) { granted ->
            onPermissionResult(granted)
            coroutineScope.launch {
                preferencesManager.setFirstLaunchComplete()
            }
        }

        // Request permission on first launch
        LaunchedEffect(isFirstLaunch) {
            if (isFirstLaunch && !notificationPermissionState.status.isGranted) {
                showPermissionDialog = true
            } else if (isFirstLaunch) {
                // If already granted, just mark first launch as complete
                preferencesManager.setFirstLaunchComplete()
            }
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPermissionDialog = false
                    coroutineScope.launch {
                        preferencesManager.setFirstLaunchComplete()
                    }
                },
                title = { Text("Enable Notifications") },
                text = {
                    Text("Utility Cam can notify you when expired photos are automatically deleted. Would you like to enable notifications?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        notificationPermissionState.launchPermissionRequest()
                    }) {
                        Text("Allow")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        coroutineScope.launch {
                            preferencesManager.setFirstLaunchComplete()
                        }
                    }) {
                        Text("Not Now")
                    }
                }
            )
        }
    } else {
        // For Android 12 and below, just mark first launch as complete
        LaunchedEffect(isFirstLaunch) {
            if (isFirstLaunch) {
                preferencesManager.setFirstLaunchComplete()
            }
        }
    }
}

/**
 * Check if notification permission is granted
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun isNotificationPermissionGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true // No runtime permission needed for Android 12 and below
    }

    val notificationPermissionState = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS
    )

    return notificationPermissionState.status.isGranted
}

/**
 * Request notification permission with a button click
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestNotificationPermission(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        ) { granted ->
            onPermissionResult(granted)
        }

        // Show rationale if needed
        if (showRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showRationaleDialog = false },
                title = { Text("Notification Permission Required") },
                text = {
                    Text("To receive notifications about deleted photos, please enable notification permission in app settings.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showRationaleDialog = false
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRationaleDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Launch permission request
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                if (notificationPermissionState.status.shouldShowRationale) {
                    // User has denied permission before, show rationale
                    showRationaleDialog = true
                } else {
                    // First time or user hasn't permanently denied
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}
