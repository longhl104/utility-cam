package com.utility.cam.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
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
    private const val TAG = "NotificationHelper"

    /**
     * Creates the notification channel (required for Android O and above)
     */
    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created with importance: $importance")
    }

    /**
     * Sends a notification about deleted photos
     */
    fun sendPhotoCleanupNotification(context: Context, deletedCount: Int) {
        val prepMessage = "Preparing to send notification for $deletedCount deleted photo(s)"
        Log.d(TAG, prepMessage)

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

        val contentMessage = "Notification content - Title: $title, Message: $message"
        Log.d(TAG, contentMessage)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        val sentMessage = "Notification sent with ID: $NOTIFICATION_ID"
        Log.d(TAG, sentMessage)
    }
}
