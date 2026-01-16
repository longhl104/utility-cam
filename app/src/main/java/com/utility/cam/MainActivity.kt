package com.utility.cam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.utility.cam.ui.navigation.UtilityCamNavigation
import com.utility.cam.ui.theme.UtilityCamTheme
import com.utility.cam.worker.PhotoCleanupWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule periodic cleanup worker
        schedulePhotoCleanup()
        
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
        val cleanupRequest = PeriodicWorkRequestBuilder<PhotoCleanupWorker>(
            15, TimeUnit.MINUTES
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "photo_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    companion object {
        const val EXTRA_PHOTO_ID = "extra_photo_id"
    }
}
