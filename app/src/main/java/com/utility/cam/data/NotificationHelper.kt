package com.utility.cam.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.utility.cam.MainActivity
import com.utility.cam.R

/**
 * Helper class for managing notifications
 */
object NotificationHelper {

    private const val CHANNEL_ID = "photo_cleanup_channel"
    private const val CHANNEL_NAME = "Photo Cleanup"
    private const val CHANNEL_DESCRIPTION = "Notifications about automatic photo cleanup"
    private const val NOTIFICATION_ID = 1001

    /**
     * Creates the notification channel (required for Android O and above)
     */
    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Sends a notification about deleted photos
     */
    fun sendPhotoCleanupNotification(context: Context, deletedCount: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_cleanup_title)
        val message = context.resources.getQuantityString(
            R.plurals.notification_cleanup_message,
            deletedCount,
            deletedCount
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
