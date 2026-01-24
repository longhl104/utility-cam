package com.utility.cam.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Manager for handling in-app reviews using Google Play In-App Review API.
 *
 * This follows Google's best practices for in-app reviews:
 * - Pre-cache ReviewInfo ahead of time
 * - Launch review flow only when ready
 * - Provide fallback to Play Store if review API fails
 */
class InAppReviewManager(private val context: Context) {
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
    private var cachedReviewInfo: ReviewInfo? = null

    companion object {
        private const val TAG = "InAppReviewManager"
    }

    /**
     * Pre-cache the ReviewInfo object ahead of time.
     * Call this before you're ready to show the review flow.
     *
     * Note: The ReviewInfo object is only valid for a limited amount of time.
     * Request it only when you are certain your app will launch the review flow soon.
     */
    fun preCacheReviewInfo(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val request = reviewManager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cachedReviewInfo = task.result
                Log.d(TAG, "ReviewInfo pre-cached successfully")
                onSuccess()
            } else {
                val exception = task.exception
                Log.e(TAG, "Failed to pre-cache ReviewInfo", exception)
                onFailure(exception ?: Exception("Unknown error"))
            }
        }
    }

    /**
     * Launch the in-app review flow.
     *
     * If ReviewInfo is not cached, this will request it on-demand.
     * The in-app review API may not always show the dialog (Google's decision based on quotas/conditions).
     *
     * @param activity The activity to launch the review flow from (required)
     * @param onComplete Callback when the review flow is complete (doesn't indicate if user reviewed)
     * @param onFallback Callback to handle fallback scenario (e.g., open Play Store)
     */
    fun launchReviewFlow(
        activity: Activity,
        onComplete: () -> Unit = {},
        onFallback: () -> Unit = {}
    ) {
        // If we have cached ReviewInfo, use it; otherwise request it now
        if (cachedReviewInfo != null) {
            startReviewFlow(activity, cachedReviewInfo!!, onComplete, onFallback)
        } else {
            // Request ReviewInfo on-demand if not cached
            Log.d(TAG, "ReviewInfo not cached, requesting now")
            val request = reviewManager.requestReviewFlow()

            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    Log.d(TAG, "ReviewInfo requested successfully")
                    startReviewFlow(activity, reviewInfo, onComplete, onFallback)
                } else {
                    Log.e(TAG, "Failed to request ReviewInfo", task.exception)
                    onFallback()
                }
            }

            request.addOnFailureListener { exception ->
                Log.e(TAG, "ReviewInfo request failed", exception)
                onFallback()
            }
        }
    }

    /**
     * Internal method to start the review flow with a ReviewInfo object.
     */
    private fun startReviewFlow(
        activity: Activity,
        reviewInfo: ReviewInfo,
        onComplete: () -> Unit,
        onFallback: () -> Unit
    ) {
        Log.d(TAG, "Starting review flow")
        val flow = reviewManager.launchReviewFlow(activity, reviewInfo)

        flow.addOnCompleteListener { _ ->
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown. Thus, no
            // matter the result, we continue our app flow.
            Log.d(TAG, "Review flow completed")
            onComplete()

            // Clear cached ReviewInfo after use (it's only valid once)
            cachedReviewInfo = null
        }

        flow.addOnFailureListener { exception ->
            Log.e(TAG, "Review flow failed: ${exception.message}", exception)
            onFallback()

            // Clear cached ReviewInfo after failure
            cachedReviewInfo = null
        }
    }

    /**
     * Opens the app's page in Google Play Store as a fallback.
     * This is useful when the in-app review API is not available or fails.
     */
    fun openPlayStoreForReview() {
        try {
            // Try to open in Play Store app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "market://details?id=${context.packageName}".toUri()
                setPackage("com.android.vending")
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened Play Store app for review")
        } catch (e: Exception) {
            // Fallback to browser if Play Store is not installed
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                }
                context.startActivity(intent)
                Log.d(TAG, "Opened Play Store in browser for review")
            } catch (e2: Exception) {
                Log.e(TAG, "Unable to open Play Store", e2)
            }
        }
    }
}
