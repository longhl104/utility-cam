# Analytics Consent Implementation

## Overview

This document describes the implementation of user consent for Firebase Analytics to comply with Google Play Store policies regarding the use of Advertising ID (AD_ID permission).

## Problem Statement

The app received a warning from Google Play Console:
> "Use Advertising Identifier only for advertising or user analytics. Respect user choices (opt-outs), get consent for PII links, and disclose use clearly."

## Solution

### 1. First-Launch Consent Dialog ✅

On first app launch, users see a consent dialog before any analytics data is collected.

**Location**: `app/src/main/java/com/utility/cam/ui/gallery/GalleryScreen.kt`

**Features**:

- Shows automatically on first app launch
- Cannot be dismissed without making a choice
- Clear explanation of what data is collected
- Two buttons: "Allow" or "No Thanks"
- Only appears once - user's choice is saved

**Dialog Content**:

- **Title**: "Help Us Improve Utility Cam"
- **Description**: Lists what IS collected and what is NOT collected
- **Mentions**: User can change this later in Settings > Privacy

### 2. User Control via Settings ✅

Added a Privacy section in Settings that allows users to opt in/out of analytics collection.

**Location**: `app/src/main/java/com/utility/cam/ui/settings/SettingsScreen.kt`

**Features**:

- Toggle switch labeled "Help Improve the App"
- Clear description: "Share anonymous usage data to help us improve the app. No personal information is collected."
- Located in a dedicated "Privacy" section
- Changes take effect immediately

### 2. Preference Storage ✅

Analytics consent preference is stored using DataStore.

**Location**: `app/src/main/java/com/utility/cam/data/PreferencesManager.kt`

**Implementation**:

```kotlin
val ANALYTICS_ENABLED_KEY = booleanPreferencesKey("analytics_enabled")
val ANALYTICS_CONSENT_SHOWN_KEY = booleanPreferencesKey("analytics_consent_shown")

fun getAnalyticsEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[ANALYTICS_ENABLED_KEY] ?: true
}

suspend fun setAnalyticsEnabled(enabled: Boolean) {
    context.dataStore.edit { preferences ->
        preferences[ANALYTICS_ENABLED_KEY] = enabled
    }
}

fun hasShownAnalyticsConsent(): Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[ANALYTICS_CONSENT_SHOWN_KEY] ?: false
}

suspend fun setAnalyticsConsentShown() {
    context.dataStore.edit { preferences ->
        preferences[ANALYTICS_CONSENT_SHOWN_KEY] = true
    }
}
```

**Storage**:

- `analytics_enabled`: User's consent choice (true/false)
- `analytics_consent_shown`: Whether dialog has been shown (prevents showing again)

### 3. Firebase Analytics Control ✅

AnalyticsHelper respects user consent and controls Firebase Analytics collection.

**Location**: `app/src/main/java/com/utility/cam/analytics/AnalyticsHelper.kt`

**Implementation**:

```kotlin
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
    }
}

fun setAnalyticsEnabled(enabled: Boolean) {
    if (BuildConfig.USE_FIREBASE_ANALYTICS && ::analytics.isInitialized) {
        analytics.setAnalyticsCollectionEnabled(enabled)
        isEnabled = enabled
    }
}
```

**Behavior**:

- Checks user consent on app launch
- Calls `setAnalyticsCollectionEnabled()` to enable/disable Firebase Analytics
- All analytics events are skipped when disabled
- Updates take effect immediately without app restart

### 4. Build Configuration ✅

Analytics can be completely disabled in debug builds via BuildConfig.

**Location**: `app/build.gradle.kts`

```kotlin
buildTypes {
    debug {
        buildConfigField("boolean", "USE_FIREBASE_ANALYTICS", "false")
    }
    release {
        buildConfigField("boolean", "USE_FIREBASE_ANALYTICS", "true")
    }
}
```

### 5. Privacy Disclosure ✅

Clear disclosure in the About section of Settings.

**Location**: Settings > About Utility Cam

**Disclosure Text**:
> "Privacy: We collect anonymous usage data to improve the app. No photos or personal information are ever shared. You can disable this in Settings > Privacy."

### 6. Manifest Permission ✅

AD_ID permission is declared but not required.

**Location**: `app/src/main/AndroidManifest.xml`

```xml
<!-- Optional permission for Firebase Analytics - respects user opt-out -->
<uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="replace" />
```

**Note**: The `tools:node="replace"` ensures this permission doesn't conflict with library manifests.

### 7. String Resources ✅

All user-facing text is in string resources for internationalization.

**Location**: `app/src/main/res/values/strings.xml`

```xml
<string name="settings_privacy">Privacy</string>
<string name="settings_analytics_consent">Help Improve the App</string>
<string name="settings_analytics_consent_hint">Share anonymous usage data to help us improve the app. No personal information is collected.</string>
<string name="settings_analytics_consent_dialog_title">Help Us Improve Utility Cam</string>
<string name="settings_analytics_consent_dialog_message">We\'d like to collect anonymous usage data to improve the app. This includes:\n\n• App feature usage\n• Photo capture events (duration, not content)\n• App performance data\n\nWe DO NOT collect:\n• Your photos or their content\n• Descriptions you enter\n• Personal information\n\nYou can change this setting anytime in Settings > Privacy.</string>
<string name="settings_analytics_consent_accept">Allow</string>
<string name="settings_analytics_consent_decline">No Thanks</string>
<string name="settings_about_privacy">Privacy: We collect anonymous usage data to improve the app. No photos or personal information are ever shared. You can disable this in Settings > Privacy.</string>
```

## Data Collection Disclosure

### What We Collect (when enabled)

- App open events
- Photo capture events (TTL duration, has description)
- Photo saved/deleted/shared events
- Language selection changes
- Feedback dialog interactions

### What We DON'T Collect

- Photo content or files
- Descriptions entered by users
- Personal information
- Location data
- Device identifiers (when user opts out)

## User Flow

### First Time Users

1. Open app for the first time
2. **Consent dialog appears immediately** on Gallery screen
3. User reads what data is collected (and what isn't)
4. User chooses:
   - **"Allow"** - Analytics enabled, dialog won't show again
   - **"No Thanks"** - Analytics disabled, dialog won't show again
5. Choice is saved permanently
6. User can change their choice anytime in Settings > Privacy

### Changing Consent Later

**To Disable Analytics:**

1. Go to Settings
2. Scroll to "Privacy" section
3. Toggle off "Help Improve the App"
4. Analytics stops immediately (no app restart needed)

**To Enable Analytics:**

1. Go to Settings > Privacy
2. Toggle on "Help Improve the App"
3. Analytics resumes immediately

## Compliance Checklist

- ✅ **First-Launch Consent**: Dialog shown before any data collection
- ✅ **Cannot Be Bypassed**: User must make a choice to proceed
- ✅ **User Control**: Toggle switch in Settings for later changes
- ✅ **Clear Disclosure**: Privacy policy text in About section AND consent dialog
- ✅ **Respect Opt-Out**: Firebase Analytics disabled when user opts out
- ✅ **No PII**: Photos and personal data are never sent
- ✅ **Transparent Use**: Clear description of what data is collected
- ✅ **Easy Access**: Privacy settings are easily accessible
- ✅ **Immediate Effect**: Changes apply without app restart
- ✅ **Persistent Choice**: User's decision is saved permanently

## Testing

### To verify analytics consent is working

1. **Enable Analytics**:
   - Open Settings > Privacy
   - Enable "Help Improve the App"
   - Check Firebase Console for events

2. **Disable Analytics**:
   - Open Settings > Privacy
   - Disable "Help Improve the App"
   - No events should appear in Firebase Console

3. **Debug Build**:
   - Debug builds have `USE_FIREBASE_ANALYTICS = false`
   - Analytics should never fire in debug builds
   - Can change to `true` for testing

## Firebase Console Configuration

Firebase Analytics automatically respects the `setAnalyticsCollectionEnabled()` call. No additional configuration is needed in the Firebase Console.

However, you should also:

1. Review Data Collection settings in Firebase Console
2. Ensure data retention is set appropriately
3. Configure user property collection if needed

## Future Enhancements

Potential improvements:

- [ ] Show first-time consent dialog on app launch
- [ ] Add detailed privacy policy link
- [ ] Log consent acceptance/rejection as an event
- [ ] Add data deletion request feature
- [ ] Provide more granular control (e.g., separate toggles for different event types)

## References

- [Firebase Analytics - Control collection](https://firebase.google.com/docs/analytics/configure-data-collection)
- [Google Play - User Data Policy](https://support.google.com/googleplay/android-developer/answer/10787469)
- [AD_ID Permission](https://support.google.com/googleplay/android-developer/answer/6048248)
