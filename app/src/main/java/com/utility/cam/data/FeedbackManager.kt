package com.utility.cam.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.feedbackDataStore by preferencesDataStore(name = "feedback_preferences")

/**
 * Manages in-app feedback/review requests
 */
class FeedbackManager(private val context: Context) {

    companion object {
        private val PHOTO_COUNT_KEY = intPreferencesKey("photo_count")
        private val APP_LAUNCH_COUNT_KEY = intPreferencesKey("app_launch_count")
        private val SAVED_PHOTOS_COUNT_KEY = intPreferencesKey("saved_photos_count")
        private val LAST_PROMPT_TIME_KEY = longPreferencesKey("last_prompt_time")
        private val LAST_SAVE_REVIEW_TIME_KEY = longPreferencesKey("last_save_review_time")
        private val HAS_RATED_KEY = booleanPreferencesKey("has_rated")
        private val DISMISSED_COUNT_KEY = intPreferencesKey("dismissed_count")

        // Thresholds for showing feedback prompt
        private const val MIN_PHOTOS_TAKEN = 3
        private const val MIN_APP_LAUNCHES = 5
        private const val DAYS_BETWEEN_PROMPTS = 7
        private const val MAX_DISMISSALS = 2

        // Thresholds for in-app review after saving photos
        private const val PHOTOS_SAVED_FOR_REVIEW = 5 // Trigger review after saving 5 photos
        private const val DAYS_BETWEEN_SAVE_REVIEWS = 30 // Don't trigger save-based review more than once per 30 days
    }

    /**
     * Increment photo count
     */
    suspend fun incrementPhotoCount() {
        context.feedbackDataStore.edit { preferences ->
            val current = preferences[PHOTO_COUNT_KEY] ?: 0
            preferences[PHOTO_COUNT_KEY] = current + 1
        }
    }

    /**
     * Increment saved photos count
     */
    suspend fun incrementSavedPhotoCount(count: Int = 1) {
        context.feedbackDataStore.edit { preferences ->
            val current = preferences[SAVED_PHOTOS_COUNT_KEY] ?: 0
            preferences[SAVED_PHOTOS_COUNT_KEY] = current + count
        }
    }

    /**
     * Check if we should trigger in-app review after saving photos.
     * This is a separate flow from the feedback prompt dialog.
     */
    suspend fun shouldTriggerReviewAfterSave(): Boolean {
        var shouldTrigger = false

        context.feedbackDataStore.edit { preferences ->
            val hasRated = preferences[HAS_RATED_KEY] ?: false
            if (hasRated) {
                shouldTrigger = false
                return@edit
            }

            val savedCount = preferences[SAVED_PHOTOS_COUNT_KEY] ?: 0
            val lastSaveReviewTime = preferences[LAST_SAVE_REVIEW_TIME_KEY] ?: 0L

            // Check if enough time has passed since last save-based review
            val daysSinceLastReview = (System.currentTimeMillis() - lastSaveReviewTime) / (1000 * 60 * 60 * 24)

            if (savedCount >= PHOTOS_SAVED_FOR_REVIEW &&
                (lastSaveReviewTime == 0L || daysSinceLastReview >= DAYS_BETWEEN_SAVE_REVIEWS)) {
                shouldTrigger = true
                // Reset counter and update last review time
                preferences[SAVED_PHOTOS_COUNT_KEY] = 0
                preferences[LAST_SAVE_REVIEW_TIME_KEY] = System.currentTimeMillis()
            }
        }

        return shouldTrigger
    }

    /**
     * Increment app launch count
     */
    suspend fun incrementAppLaunchCount() {
        context.feedbackDataStore.edit { preferences ->
            val current = preferences[APP_LAUNCH_COUNT_KEY] ?: 0
            preferences[APP_LAUNCH_COUNT_KEY] = current + 1
        }
    }

    /**
     * Check if we should show feedback prompt
     */
    fun shouldShowFeedbackPrompt(): Flow<Boolean> {
        return context.feedbackDataStore.data.map { preferences ->
            val hasRated = preferences[HAS_RATED_KEY] ?: false
            if (hasRated) return@map false

            val photoCount = preferences[PHOTO_COUNT_KEY] ?: 0
            val launchCount = preferences[APP_LAUNCH_COUNT_KEY] ?: 0
            val lastPromptTime = preferences[LAST_PROMPT_TIME_KEY] ?: 0L
            val dismissedCount = preferences[DISMISSED_COUNT_KEY] ?: 0

            // Don't show if dismissed too many times
            if (dismissedCount >= MAX_DISMISSALS) return@map false

            // Check if enough time has passed since last prompt
            val daysSinceLastPrompt = (System.currentTimeMillis() - lastPromptTime) / (1000 * 60 * 60 * 24)
            if (lastPromptTime > 0 && daysSinceLastPrompt < DAYS_BETWEEN_PROMPTS) return@map false

            // Check if user has used the app enough
            photoCount >= MIN_PHOTOS_TAKEN && launchCount >= MIN_APP_LAUNCHES
        }
    }

    /**
     * Mark that feedback prompt was shown
     */
    suspend fun markPromptShown() {
        context.feedbackDataStore.edit { preferences ->
            preferences[LAST_PROMPT_TIME_KEY] = System.currentTimeMillis()
        }
    }

    /**
     * Mark that user dismissed the prompt
     */
    suspend fun markPromptDismissed() {
        context.feedbackDataStore.edit { preferences ->
            val current = preferences[DISMISSED_COUNT_KEY] ?: 0
            preferences[DISMISSED_COUNT_KEY] = current + 1
            preferences[LAST_PROMPT_TIME_KEY] = System.currentTimeMillis()
        }
    }

    /**
     * Mark that user has rated the app
     */
    suspend fun markUserRated() {
        context.feedbackDataStore.edit { preferences ->
            preferences[HAS_RATED_KEY] = true
        }
    }

    /**
     * Reset all feedback state (for debugging)
     */
    suspend fun resetFeedbackState() {
        context.feedbackDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
