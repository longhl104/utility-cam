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
        return try {
            val storageManager = PhotoStorageManager(applicationContext)

            // Move expired photos to bin
            val movedToBin = storageManager.moveExpiredPhotosToBin()

            // Permanently delete old bin items (30+ days)
            val deletedFromBin = storageManager.deletePermanentlyFromBin()

            // Send notification about moved photos if enabled and count > 0
            if (movedToBin > 0) {
                val preferencesManager = PreferencesManager(applicationContext)
                val notificationsEnabled = preferencesManager.getNotificationsEnabled().first()

                if (notificationsEnabled) {
                    NotificationHelper.sendPhotoCleanupNotification(applicationContext, movedToBin)
                }
            }

            // In debug mode, schedule the next run to create a repeating cycle
            if (BuildConfig.DEBUG) {
                val preferencesManager = PreferencesManager(applicationContext)
                val delaySeconds = preferencesManager.getCleanupDelaySeconds().first()

                val nextCleanup = OneTimeWorkRequestBuilder<PhotoCleanupWorker>()
                    .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "photo_cleanup",
                    ExistingWorkPolicy.REPLACE,
                    nextCleanup
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
