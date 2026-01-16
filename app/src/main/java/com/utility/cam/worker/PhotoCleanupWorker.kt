package com.utility.cam.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.utility.cam.data.PhotoStorageManager

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
            
            // Log or notify about deleted photos if needed
            if (deletedCount > 0) {
                // Could send a notification here if desired
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
