package com.utility.cam.ui.feedback

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.utility.cam.R

/**
 * Feedback dialog that prompts users to rate the app on Google Play Store
 */
@Composable
fun FeedbackDialog(
    onRateNow: () -> Unit,
    onMaybeLater: () -> Unit,
    onNoThanks: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onMaybeLater,
        title = {
            Text(stringResource(R.string.feedback_title))
        },
        text = {
            Text(stringResource(R.string.feedback_message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRateNow()
                    // Open Play Store
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = "market://details?id=${context.packageName}".toUri()
                            setPackage("com.android.vending")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Fallback to browser if Play Store is not installed
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data =
                                "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                        }
                        context.startActivity(intent)
                    }
                }
            ) {
                Text(stringResource(R.string.feedback_rate_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onMaybeLater) {
                Text(stringResource(R.string.feedback_later))
            }
        }
    )
}
