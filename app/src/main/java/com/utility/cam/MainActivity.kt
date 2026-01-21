package com.utility.cam

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.utility.cam.data.NotificationHelper
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.LocaleManager
import com.utility.cam.ui.navigation.UtilityCamNavigation
import com.utility.cam.ui.theme.UtilityCamTheme
import com.utility.cam.worker.ExpiringPhotoReminderWorker
import com.utility.cam.worker.PhotoCleanupWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply selected language before activity is created
        val localeManager = LocaleManager(newBase)
        val selectedLanguage = runBlocking {
            localeManager.getSelectedLanguage().first()
        }

        val context = if (selectedLanguage != LocaleManager.SYSTEM_DEFAULT) {
            localeManager.applyLocale(selectedLanguage)
        } else {
            newBase
        }

        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule periodic cleanup worker
        schedulePhotoCleanup()
        
        // Schedule expiring photo reminder worker
        scheduleExpiringPhotoReminder()

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
            // Get delay from preferences
            val preferencesManager = PreferencesManager(this)
            val delaySeconds = runBlocking {
                preferencesManager.getCleanupDelaySeconds().first()
            }

            val cleanupRequest = OneTimeWorkRequestBuilder<PhotoCleanupWorker>()
                .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
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

    private fun scheduleExpiringPhotoReminder() {
        if (BuildConfig.DEBUG) {
            // In debug mode, check every 5 minutes
            val delayMinutes = 5L
            val reminderRequest = OneTimeWorkRequestBuilder<ExpiringPhotoReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "photo_expiring_reminder",
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )

            val scheduleMessage = "Scheduled expiring photo reminder worker with $delayMinutes minute delay"
            Log.d("MainActivity", scheduleMessage)
        } else {
            // In release mode, check every 30 minutes
            val delayMinutes = 30L
            val reminderRequest = OneTimeWorkRequestBuilder<ExpiringPhotoReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "photo_expiring_reminder",
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )

            val scheduleMessage = "Scheduled expiring photo reminder worker with $delayMinutes minute delay"
            Log.d("MainActivity", scheduleMessage)
        }
    }

    companion object
}
