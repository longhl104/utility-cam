package com.utility.cam.data

import java.time.Instant

/**
 * Represents a photo captured in Utility Cam
 */
data class UtilityPhoto(
    val id: String,
    val fileName: String,
    val filePath: String,
    val captureTimestamp: Long,
    val expirationTimestamp: Long,
    val thumbnailPath: String? = null,
    val description: String? = null
) {
    /**
     * Check if this photo has expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expirationTimestamp
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
enum class TTLDuration(val hours: Int, val displayName: String) {
    TWENTY_FOUR_HOURS(24, "24 hours"),
    THREE_DAYS(72, "3 days"),
    ONE_WEEK(168, "1 week");
    
    fun toMilliseconds(): Long = hours * 60 * 60 * 1000L
}
