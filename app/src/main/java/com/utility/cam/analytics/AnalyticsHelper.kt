package com.utility.cam.analytics
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.utility.cam.BuildConfig
import com.utility.cam.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object AnalyticsHelper {
    private lateinit var analytics: FirebaseAnalytics
    private var isEnabled = false
    private lateinit var preferencesManager: PreferencesManager

    fun initialize(context: Context) {
        preferencesManager = PreferencesManager(context)

        // Only initialize if build type allows it
        if (BuildConfig.USE_FIREBASE_ANALYTICS) {
            analytics = Firebase.analytics

            // Check user consent
            val userConsent = runBlocking {
                preferencesManager.getAnalyticsEnabled().first()
            }

            // Set analytics collection enabled based on user consent
            analytics.setAnalyticsCollectionEnabled(userConsent)
            isEnabled = userConsent
        } else {
            isEnabled = false
        }
    }

    /**
     * Update analytics consent when user changes settings
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        if (BuildConfig.USE_FIREBASE_ANALYTICS && ::analytics.isInitialized) {
            analytics.setAnalyticsCollectionEnabled(enabled)
            isEnabled = enabled
        }
    }

    fun logPhotoCaptured(ttlDuration: String, hasDescription: Boolean) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("ttl_duration", ttlDuration)
            putBoolean("has_description", hasDescription)
        }
        analytics.logEvent("photo_captured", bundle)
    }
    fun logPhotoSavedToGallery(photoId: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("photo_id", photoId)
        }
        analytics.logEvent("photo_saved_to_gallery", bundle)
    }
    fun logPhotoDeleted(photoId: String, manualDelete: Boolean = true) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("photo_id", photoId)
            putBoolean("manual_delete", manualDelete)
        }
        analytics.logEvent("photo_deleted", bundle)
    }
    fun logPhotoShared(photoId: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("photo_id", photoId)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)
    }
    fun logPhotosAutoCleaned(count: Int) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("photo_count", count)
        }
        analytics.logEvent("photos_auto_cleaned", bundle)
    }
    fun logSettingChanged(settingName: String, value: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("setting_name", settingName)
            putString("value", value)
        }
        analytics.logEvent("setting_changed", bundle)
    }
    fun logLanguageChanged(oldLanguage: String, newLanguage: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("old_language", oldLanguage)
            putString("new_language", newLanguage)
        }
        analytics.logEvent("language_changed", bundle)
    }
    fun logFeedbackAction(action: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("action", action)
        }
        analytics.logEvent("feedback_action", bundle)
    }
    fun logScreenView(screenName: String, screenClass: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
    fun logNotificationSettingChanged(notificationType: String, enabled: Boolean) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("notification_type", notificationType)
            putBoolean("enabled", enabled)
        }
        analytics.logEvent("notification_setting_changed", bundle)
    }
    fun logWidgetInteraction(action: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("action", action)
        }
        analytics.logEvent("widget_interaction", bundle)
    }
    fun logAppLaunched() {
        if (!isEnabled) return
        analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }
    fun setUserProperty(propertyName: String, value: String) {
        if (!isEnabled) return
        analytics.setUserProperty(propertyName, value)
    }
    fun logCameraFeatureUsed(feature: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("feature", feature)
        }
        analytics.logEvent("camera_feature_used", bundle)
    }

    fun logBatchDelete(count: Int) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("photo_count", count)
        }
        analytics.logEvent("batch_delete", bundle)
    }

    fun logBatchSave(count: Int) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("photo_count", count)
        }
        analytics.logEvent("batch_save", bundle)
    }

    fun logBatchShare(count: Int) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("photo_count", count)
        }
        analytics.logEvent("batch_share", bundle)
    }

    // Bin operations
    fun logBinItemRestored(count: Int = 1) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("item_count", count)
        }
        analytics.logEvent("bin_item_restored", bundle)
    }

    fun logBinItemDeletedPermanently(count: Int = 1) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("item_count", count)
        }
        analytics.logEvent("bin_deleted_permanently", bundle)
    }

    fun logBinEmptied(itemCount: Int) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("item_count", itemCount)
        }
        analytics.logEvent("bin_emptied", bundle)
    }

    // PDF generation
    fun logPdfGenerated(imageCount: Int) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putInt("image_count", imageCount)
        }
        analytics.logEvent("pdf_generated", bundle)
    }

    fun logPdfShared() {
        if (!isEnabled) return
        analytics.logEvent("pdf_shared", null)
    }

    // Pro feature interactions
    fun logProScreenViewed() {
        if (!isEnabled) return
        analytics.logEvent("pro_screen_viewed", null)
    }

    fun logProPurchaseInitiated() {
        if (!isEnabled) return
        analytics.logEvent("pro_purchase_initiated", null)
    }

    fun logProPurchaseCompleted() {
        if (!isEnabled) return
        analytics.logEvent("pro_purchase_completed", null)
    }

    fun logProPurchaseFailed(reason: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("reason", reason)
        }
        analytics.logEvent("pro_purchase_failed", bundle)
    }

    fun logProFeatureAttempted(featureName: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("feature_name", featureName)
        }
        analytics.logEvent("pro_feature_attempted", bundle)
    }

    // Video capture
    fun logVideoCaptured(duration: Long) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putLong("duration_ms", duration)
        }
        analytics.logEvent("video_captured", bundle)
    }

    fun logVideoSavedToGallery(videoId: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("video_id", videoId)
        }
        analytics.logEvent("video_saved_to_gallery", bundle)
    }

    // Theme changes
    fun logThemeChanged(oldTheme: String, newTheme: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("old_theme", oldTheme)
            putString("new_theme", newTheme)
        }
        analytics.logEvent("theme_changed", bundle)
    }

    // Gallery sorting
    fun logGallerySortChanged(sortMode: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("sort_mode", sortMode)
        }
        analytics.logEvent("gallery_sort_changed", bundle)
    }

    // Biometric settings
    fun logBiometricLockEnabled(enabled: Boolean) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putBoolean("enabled", enabled)
        }
        analytics.logEvent("biometric_lock_toggled", bundle)
    }

    fun logBiometricAuthenticationAttempt(success: Boolean) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putBoolean("success", success)
        }
        analytics.logEvent("biometric_auth_attempt", bundle)
    }

    // In-app review
    fun logInAppReviewTriggered() {
        if (!isEnabled) return
        analytics.logEvent("in_app_review_triggered", null)
    }

    fun logInAppReviewCompleted() {
        if (!isEnabled) return
        analytics.logEvent("in_app_review_completed", null)
    }

    // Navigation
    fun logNavigationDrawerOpened() {
        if (!isEnabled) return
        analytics.logEvent("navigation_drawer_opened", null)
    }

    fun logNavigationItemClicked(destination: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("destination", destination)
        }
        analytics.logEvent("navigation_item_clicked", bundle)
    }

    // Custom TTL
    fun logCustomTTLSet(duration: Long, unit: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putLong("duration", duration)
            putString("unit", unit)
        }
        analytics.logEvent("custom_ttl_set", bundle)
    }

    // Language download
    fun logLanguageDownloadStarted(languageCode: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("language_code", languageCode)
        }
        analytics.logEvent("language_download_started", bundle)
    }

    fun logLanguageDownloadCompleted(languageCode: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("language_code", languageCode)
        }
        analytics.logEvent("language_download_completed", bundle)
    }

    fun logLanguageDownloadFailed(languageCode: String, reason: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("language_code", languageCode)
            putString("reason", reason)
        }
        analytics.logEvent("language_download_failed", bundle)
    }

    // Media detail actions
    fun logMediaDetailViewed(mediaId: String, mediaType: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("media_id", mediaId)
            putString("media_type", mediaType)
        }
        analytics.logEvent("media_detail_viewed", bundle)
    }

    fun logMediaKeptForever(mediaId: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("media_id", mediaId)
        }
        analytics.logEvent("media_kept_forever", bundle)
    }

    // Drawer navigation
    fun logUpgradeToProClicked(source: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("source", source)
        }
        analytics.logEvent("upgrade_to_pro_clicked", bundle)
    }

    // Debug features
    fun logDebugProOverrideToggled(enabled: Boolean) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putBoolean("enabled", enabled)
        }
        analytics.logEvent("debug_pro_override_toggled", bundle)
    }

    // AdMob events
    fun logAdLoaded(screenName: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("screen_name", screenName)
        }
        analytics.logEvent("ad_loaded", bundle)
    }

    fun logAdLoadFailed(screenName: String, errorMessage: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("screen_name", screenName)
            putString("error_message", errorMessage)
        }
        analytics.logEvent("ad_load_failed", bundle)
    }

    fun logAdClicked(screenName: String) {
        if (!isEnabled) return
        val bundle = Bundle().apply {
            putString("screen_name", screenName)
        }
        analytics.logEvent("ad_clicked", bundle)
    }
}
