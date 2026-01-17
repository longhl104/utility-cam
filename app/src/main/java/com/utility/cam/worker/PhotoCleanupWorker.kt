package com.utility.cam.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.utility.cam.data.NotificationHelper
import com.utility.cam.data.PhotoStorageManager
import com.utility.cam.data.PreferencesManager
import kotlinx.coroutines.flow.first

/**
 * Background worker that periodically deletes expired photos
 */
class PhotoCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val storageManager = PhotoStorageManager(applicationContext)
            val deletedCount = storageManager.deleteExpiredPhotos()
            
            // Send notification about deleted photos if enabled
            if (deletedCount > 0) {
                val preferencesManager = PreferencesManager(applicationContext)
                val notificationsEnabled = preferencesManager.getNotificationsEnabled().first()

                if (notificationsEnabled) {
                    NotificationHelper.sendPhotoCleanupNotification(applicationContext, deletedCount)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
