package com.utility.cam.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.utility.cam.R

/**
 * Reusable dialog shown when a non-Pro user tries to access a Pro-only feature.
 *
 * @param onDismiss Called when the dialog is dismissed
 * @param onUpgrade Called when the user clicks "Upgrade to PRO" button
 * @param titleResId String resource ID for the dialog title
 * @param messageResId String resource ID for the dialog message
 * @param upgradeButtonResId String resource ID for the upgrade button text
 * @param cancelButtonResId String resource ID for the cancel button text
 */
@Composable
fun ProLockedDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    titleResId: Int = R.string.camera_video_pro_only_title,
    messageResId: Int = R.string.camera_video_pro_only_message,
    upgradeButtonResId: Int = R.string.camera_video_pro_only_upgrade,
    cancelButtonResId: Int = R.string.camera_video_pro_only_cancel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResId)) },
        text = { Text(stringResource(messageResId)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onUpgrade()
            }) {
                Text(stringResource(upgradeButtonResId))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(cancelButtonResId))
            }
        }
    )
}
