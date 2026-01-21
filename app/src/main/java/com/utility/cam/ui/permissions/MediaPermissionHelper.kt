package com.utility.cam.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

/**
 * Helper for handling media permissions across Android versions,
 * including Android 14+ Selected Photos Access
 */

/**
 * Check if the app has media permission (photos)
 */
fun Context.hasMediaPermission(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            // Android 14+ (API 34+) - Check for either full access or partial access
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            // Android 13 (API 33)
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        }
        else -> {
            // Android 12 and below
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * Get the appropriate media permission to request based on Android version
 */
fun getMediaPermissionToRequest(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            // Android 14+ - Request both full and partial access
            // The system will show "Select photos" and "Allow all" options
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            // Android 13
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
        else -> {
            // Android 12 and below
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

/**
 * Composable to create a media permission launcher that handles Android 14+ Selected Photos Access
 */
@Composable
fun rememberMediaPermissionLauncher(
    onPermissionResult: (Boolean) -> Unit
): androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // On Android 14+, we consider it granted if either permission is granted
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
            permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
        } else {
            permissions.values.any { it }
        }
        onPermissionResult(isGranted)
    }
}

/**
 * Check if we should show rationale for media permission
 */
fun androidx.activity.ComponentActivity.shouldShowMediaPermissionRationale(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) ||
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)
        }
        else -> {
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
