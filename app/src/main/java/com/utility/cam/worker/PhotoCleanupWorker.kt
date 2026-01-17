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
 * Background worker that periodically deletes expired photos
 */
class PhotoCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        private const val TAG = "PhotoCleanupWorker"
    }

    override suspend fun doWork(): Result {
        val startMessage = "PhotoCleanupWorker started at ${System.currentTimeMillis()}"
        Log.d(TAG, startMessage)

        return try {
            val storageManager = PhotoStorageManager(applicationContext)
            val deletedCount = storageManager.deleteExpiredPhotos()
            
            val deletedMessage = "Deleted $deletedCount expired photo(s)"
            Log.d(TAG, deletedMessage)

            // Send notification about deleted photos if enabled
            if (deletedCount > 0) {
                val preferencesManager = PreferencesManager(applicationContext)
                val notificationsEnabled = preferencesManager.getNotificationsEnabled().first()

                val enabledMessage = "Notifications enabled: $notificationsEnabled"
                Log.d(TAG, enabledMessage)

                if (notificationsEnabled) {
                    NotificationHelper.sendPhotoCleanupNotification(applicationContext, deletedCount)
                } else {
                    val skippedMessage = "Notifications disabled, skipping notification"
                    Log.d(TAG, skippedMessage)
                }
            } else {
                val noDeleteMessage = "No photos deleted, no notification needed"
                Log.d(TAG, noDeleteMessage)
            }
            
            val successMessage = "PhotoCleanupWorker completed successfully"
            Log.d(TAG, successMessage)

            // In debug mode, schedule the next run to create a repeating cycle
            if (BuildConfig.DEBUG) {
                val nextCleanup = OneTimeWorkRequestBuilder<PhotoCleanupWorker>()
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "photo_cleanup",
                    ExistingWorkPolicy.REPLACE,
                    nextCleanup
                )

                val chainMessage = "Scheduled next cleanup in 10 seconds"
                Log.d(TAG, chainMessage)
            }

            Result.success()
        } catch (e: Exception) {
            val errorMessage = "PhotoCleanupWorker failed: ${e.message}"
            Log.e(TAG, errorMessage, e)
            e.printStackTrace()
            Result.retry()
        }
    }
}
