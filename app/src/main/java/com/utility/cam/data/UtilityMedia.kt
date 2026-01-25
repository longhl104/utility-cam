package com.utility.cam.data

import android.content.Context
import com.utility.cam.R

/**
 * Represents a photo or video captured in Utility Cam
 */
data class UtilityMedia(
    val id: String,
    val fileName: String,
    val filePath: String,
    val captureTimestamp: Long,
    val expirationTimestamp: Long,
    val thumbnailPath: String? = null,
    val description: String? = null,
    val inBin: Boolean = false,
    val deletedAt: Long? = null // Timestamp when moved to bin
) {
    companion object {
        const val BIN_RETENTION_DAYS = 30
        const val BIN_RETENTION_MILLIS = BIN_RETENTION_DAYS * 24 * 60 * 60 * 1000L
    }

    /**
     * Check if this photo has expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expirationTimestamp
    }

    /**
     * Check if this photo should be permanently deleted from bin
     */
    fun shouldBePermanentlyDeleted(): Boolean {
        if (!inBin || deletedAt == null) return false
        return System.currentTimeMillis() > (deletedAt + BIN_RETENTION_MILLIS)
    }

    /**
     * Get days remaining in bin before permanent deletion
     */
    fun getDaysRemainingInBin(): Int {
        if (!inBin || deletedAt == null) return 0
        val timeRemaining = (deletedAt + BIN_RETENTION_MILLIS) - System.currentTimeMillis()
        return (timeRemaining / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }

    /**
     * Get time remaining in milliseconds
     */
    fun getTimeRemaining(): Long {
        return (expirationTimestamp - System.currentTimeMillis()).coerceAtLeast(0)
    }

    /**
     * Get formatted time remaining
     */
    fun getFormattedTimeRemaining(): String {
        val remaining = getTimeRemaining()
        val hours = remaining / (1000 * 60 * 60)
        val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)

        return when {
            hours > 24 -> "${hours / 24}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

/**
 * Time-to-live duration options
 */
enum class TTLDuration(val hours: Int, val displayNameResId: Int, val isDebugOnly: Boolean = false) {
    TEST_3_SECONDS(0, R.string.ttl_test_3_seconds, isDebugOnly = true),
    TWENTY_FOUR_HOURS(24, R.string.ttl_24_hours),
    THREE_DAYS(72, R.string.ttl_3_days),
    ONE_WEEK(168, R.string.ttl_1_week);

    fun toMilliseconds(): Long {
        // Special handling for test duration
        if (this == TEST_3_SECONDS) {
            return 3 * 1000L // 3 seconds
        }
        return hours * 60 * 60 * 1000L
    }

    fun getDisplayName(context: Context): String {
        return context.getString(displayNameResId)
    }
}
