package com.utility.cam.ui.feedback

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.utility.cam.R
import com.utility.cam.data.InAppReviewManager

/**
 * Feedback dialog that prompts users to rate the app.
 * Uses Google Play In-App Review API for a seamless experience,
 * with fallback to Play Store if the API is unavailable.
 */
@Composable
fun FeedbackDialog(
    onRateNow: () -> Unit,
    onMaybeLater: () -> Unit,
    onNoThanks: () -> Unit
) {
    val context = LocalContext.current
    val inAppReviewManager = remember { InAppReviewManager(context) }

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

                    // Try Google Play In-App Review API first
                    val activity = context as? Activity
                    if (activity != null) {
                        Log.d("FeedbackDialog", "Attempting In-App Review flow")

                        inAppReviewManager.launchReviewFlow(
                            activity = activity,
                            onComplete = {
                                Log.d("FeedbackDialog", "Review flow completed")
                                // Note: In-App Review API doesn't indicate if dialog was shown
                            },
                            onFallback = {
                                Log.w("FeedbackDialog", "Review flow not available, opening Play Store")
                                Toast.makeText(
                                    context,
                                    "Opening Play Store...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                inAppReviewManager.openPlayStoreForReview()
                            }
                        )
                    } else {
                        // No activity context, fallback to Play Store
                        Log.w("FeedbackDialog", "No activity context, opening Play Store directly")
                        inAppReviewManager.openPlayStoreForReview()
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

