package com.utility.cam

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.utility.cam.data.NotificationHelper
import com.utility.cam.ui.navigation.UtilityCamNavigation
import com.utility.cam.ui.theme.UtilityCamTheme
import com.utility.cam.worker.PhotoCleanupWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule periodic cleanup worker
        schedulePhotoCleanup()
        
        // In debug mode, also schedule immediate cleanup for testing
        if (BuildConfig.DEBUG) {
            val debugMessage = "Debug mode: Scheduling immediate cleanup check"
            Log.d("MainActivity", debugMessage)
            scheduleImmediateCleanup()
        }

        // Get photo ID from intent extras (from widget click)
        val photoId = intent?.extras?.getString("photo_id")

        setContent {
            UtilityCamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UtilityCamNavigation(initialPhotoId = photoId)
                }
            }
        }
    }
    
    private fun schedulePhotoCleanup() {
        if (BuildConfig.DEBUG) {
            // In debug mode, use chained one-time work to bypass 15-minute minimum
            // Schedule repeating 10-second cleanup cycles
            val delaySeconds = 10L
            val cleanupRequest = OneTimeWorkRequestBuilder<PhotoCleanupWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "photo_cleanup",
                ExistingWorkPolicy.REPLACE,
                cleanupRequest
            )

            val scheduleMessage = "Scheduled cleanup worker (one-time chain) with $delaySeconds second delay"
            Log.d("MainActivity", scheduleMessage)
        } else {
            // In release mode, use standard 15-minute periodic work
            val intervalMinutes = 15L
            val cleanupRequest = PeriodicWorkRequestBuilder<PhotoCleanupWorker>(
                intervalMinutes, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "photo_cleanup",
                ExistingPeriodicWorkPolicy.REPLACE,
                cleanupRequest
            )

            val scheduleMessage = "Scheduled periodic cleanup worker with $intervalMinutes minute interval"
            Log.d("MainActivity", scheduleMessage)
        }
    }

    private fun scheduleImmediateCleanup() {
        val immediateCleanup = OneTimeWorkRequestBuilder<PhotoCleanupWorker>().build()
        WorkManager.getInstance(this).enqueue(immediateCleanup)
        Log.d("MainActivity", "Scheduled immediate cleanup")
    }

    companion object
}
