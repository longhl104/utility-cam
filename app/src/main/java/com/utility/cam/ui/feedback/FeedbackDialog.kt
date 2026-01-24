package com.utility.cam.ui.feedback

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewManagerFactory
import com.utility.cam.R

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
                        val reviewManager = ReviewManagerFactory.create(context)
                        val request = reviewManager.requestReviewFlow()

                        request.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("FeedbackDialog", "Review request successful, launching flow")
                                val reviewInfo = task.result
                                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)

                                flow.addOnCompleteListener {
                                    Log.d("FeedbackDialog", "Review flow completed")
                                    // Note: In-App Review API doesn't work in all environments
                                    // (debug builds, emulators, quotas exceeded)
                                    // If it silently failed, user sees nothing - that's expected behavior
                                }

                                flow.addOnFailureListener { exception ->
                                    Log.e("FeedbackDialog", "Review flow failed: ${exception.message}")
                                    openPlayStore(context)
                                }
                            } else {
                                Log.e("FeedbackDialog", "Review request failed: ${task.exception?.message}, opening Play Store")
                                openPlayStore(context)
                            }
                        }

                        request.addOnFailureListener { exception ->
                            Log.e("FeedbackDialog", "Review request exception: ${exception.message}")
                            openPlayStore(context)
                        }
                    } else {
                        // No activity context, fallback to Play Store
                        Log.w("FeedbackDialog", "No activity context, opening Play Store directly")
                        openPlayStore(context)
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

/**
 * Opens the app's page in Google Play Store
 */
private fun openPlayStore(context: android.content.Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=${context.packageName}".toUri()
            setPackage("com.android.vending")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to browser if Play Store is not installed
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Log.e("FeedbackDialog", "Unable to open Play Store")
        }
    }
}
