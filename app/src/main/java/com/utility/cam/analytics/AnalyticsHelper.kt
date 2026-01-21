package com.utility.cam.analytics
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

object AnalyticsHelper {
    private lateinit var analytics: FirebaseAnalytics
    fun initialize(context: Context) {
        analytics = Firebase.analytics
    }
    fun logPhotoCaptured(ttlDuration: String, hasDescription: Boolean) {
        val bundle = Bundle().apply {
            putString("ttl_duration", ttlDuration)
            putBoolean("has_description", hasDescription)
        }
        analytics.logEvent("photo_captured", bundle)
    }
    fun logPhotoSavedToGallery(photoId: String) {
        val bundle = Bundle().apply {
            putString("photo_id", photoId)
        }
        analytics.logEvent("photo_saved_to_gallery", bundle)
    }
    fun logPhotoDeleted(photoId: String, manualDelete: Boolean = true) {
        val bundle = Bundle().apply {
            putString("photo_id", photoId)
            putBoolean("manual_delete", manualDelete)
        }
        analytics.logEvent("photo_deleted", bundle)
    }
    fun logPhotoShared(photoId: String) {
        val bundle = Bundle().apply {
            putString("photo_id", photoId)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)
    }
    fun logPhotosAutoCleaned(count: Int) {
        val bundle = Bundle().apply {
            putInt("photo_count", count)
        }
        analytics.logEvent("photos_auto_cleaned", bundle)
    }
    fun logSettingChanged(settingName: String, value: String) {
        val bundle = Bundle().apply {
            putString("setting_name", settingName)
            putString("value", value)
        }
        analytics.logEvent("setting_changed", bundle)
    }
    fun logLanguageChanged(oldLanguage: String, newLanguage: String) {
        val bundle = Bundle().apply {
            putString("old_language", oldLanguage)
            putString("new_language", newLanguage)
        }
        analytics.logEvent("language_changed", bundle)
    }
    fun logFeedbackAction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action)
        }
        analytics.logEvent("feedback_action", bundle)
    }
    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
    fun logNotificationSettingChanged(notificationType: String, enabled: Boolean) {
        val bundle = Bundle().apply {
            putString("notification_type", notificationType)
            putBoolean("enabled", enabled)
        }
        analytics.logEvent("notification_setting_changed", bundle)
    }
    fun logWidgetInteraction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action)
        }
        analytics.logEvent("widget_interaction", bundle)
    }
    fun logAppLaunched() {
        analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }
    fun setUserProperty(propertyName: String, value: String) {
        analytics.setUserProperty(propertyName, value)
    }
    fun logCameraFeatureUsed(feature: String) {
        val bundle = Bundle().apply {
            putString("feature", feature)
        }
        analytics.logEvent("camera_feature_used", bundle)
    }
}
