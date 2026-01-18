package com.utility.cam.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.utility.cam.BuildConfig
import com.utility.cam.data.NotificationHelper
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background worker that checks for photos expiring soon and sends reminder notifications
 */
class ExpiringPhotoReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ExpiringPhotoReminder"
        private const val REMINDER_THRESHOLD_HOURS = 1 // Notify if expiring within 1 hour
    }

    override suspend fun doWork(): Result {
        val startMessage = "ExpiringPhotoReminderWorker started at ${System.currentTimeMillis()}"
        Log.d(TAG, startMessage)

        return try {
            val preferencesManager = PreferencesManager(applicationContext)
            val reminderEnabled = preferencesManager.getReminderNotificationsEnabled().first()

            if (!reminderEnabled) {
                Log.d(TAG, "Reminder notifications disabled, skipping check")
                scheduleNextRun()
                return Result.success()
            }

            val storageManager = PhotoStorageManager(applicationContext)
            val photos = storageManager.getAllPhotos()

            Log.d(TAG, "Checking ${photos.size} photos for expiration reminders")

            val thresholdMillis = REMINDER_THRESHOLD_HOURS * 60 * 60 * 1000L
            var remindersSent = 0

            photos.forEach { photo ->
                val timeRemaining = photo.getTimeRemaining()

                // Send reminder if photo is expiring within threshold and hasn't expired yet
                if (timeRemaining > 0 && timeRemaining <= thresholdMillis) {
                    Log.d(TAG, "Photo ${photo.id} expiring in ${timeRemaining / 1000}s - sending reminder")
                    NotificationHelper.sendExpiringPhotoReminder(applicationContext, photo)
                    remindersSent++
                }
            }

            val summaryMessage = "Sent $remindersSent reminder notification(s)"
            Log.d(TAG, summaryMessage)

            scheduleNextRun()
            Result.success()
        } catch (e: Exception) {
            val errorMessage = "ExpiringPhotoReminderWorker failed: ${e.message}"
            Log.e(TAG, errorMessage, e)
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun scheduleNextRun() {
        if (BuildConfig.DEBUG) {
            // In debug mode, check every 5 minutes
            val delayMinutes = 5L
            val nextReminderCheck = OneTimeWorkRequestBuilder<ExpiringPhotoReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "photo_expiring_reminder",
                ExistingWorkPolicy.REPLACE,
                nextReminderCheck
            )

            Log.d(TAG, "Scheduled next reminder check in $delayMinutes minutes")
        } else {
            // In release mode, check every 30 minutes
            val delayMinutes = 30L
            val nextReminderCheck = OneTimeWorkRequestBuilder<ExpiringPhotoReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "photo_expiring_reminder",
                ExistingWorkPolicy.REPLACE,
                nextReminderCheck
            )

            Log.d(TAG, "Scheduled next reminder check in $delayMinutes minutes")
        }
    }
}
