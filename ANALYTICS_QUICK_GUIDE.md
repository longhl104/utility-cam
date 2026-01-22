# Analytics Consent - Quick Reference Guide

## ✅ What Was Done

### 1. User Consent Toggle

**Location**: Settings → Privacy → "Help Improve the App"

Users can enable/disable analytics at any time. Changes take effect immediately.

### 2. Privacy Disclosure

**Location**: Settings → About Utility Cam

A clear privacy notice explains:

- What data is collected (anonymous usage data)
- What is NOT collected (no photos, no personal info)
- How to disable it (Settings → Privacy)

### 3. Technical Implementation

#### Build Configuration

```kotlin
// Debug builds: Analytics DISABLED by default
debug {
    buildConfigField("boolean", "USE_FIREBASE_ANALYTICS", "false")
}

// Release builds: Analytics ENABLED (but respects user consent)
release {
    buildConfigField("boolean", "USE_FIREBASE_ANALYTICS", "true")
}
```

#### User Consent Flow

```kotlin
// On app launch
if (BuildConfig.USE_FIREBASE_ANALYTICS) {
    AnalyticsHelper.initialize(this)  // Checks user consent
    AnalyticsHelper.logAppLaunched()  // Only logs if user opted in
}

// When user changes setting
AnalyticsHelper.setAnalyticsEnabled(enabled)  // Updates Firebase immediately
```

## 🧪 Testing

### Test First-Launch Consent

1. Uninstall the app (or clear app data)
2. Install and launch the app
3. **Consent dialog should appear** on the Gallery screen
4. Choose "Allow" or "No Thanks"
5. Dialog should not appear again
6. Analytics should respect your choice

### Test User Opt-Out

1. Build and run the app (release build)
2. Go to Settings → Privacy
3. Toggle OFF "Help Improve the App"
4. Check Firebase Console - no new events should appear

### Test User Opt-In

1. Go to Settings → Privacy
2. Toggle ON "Help Improve the App"
3. Use the app (capture photos, change settings, etc.)
4. Check Firebase Console - events should appear

### Test Debug Build

1. Build in debug mode
2. Analytics should be completely disabled
3. No events in Firebase Console regardless of user setting

## 📋 Google Play Compliance

This implementation addresses the AD_ID permission warning:

✅ **User Control**: Easy toggle in Settings  
✅ **Clear Disclosure**: Privacy statement visible in About  
✅ **Respect Opt-Out**: Firebase Analytics stops when disabled  
✅ **No PII**: Photos and descriptions never sent  
✅ **Transparent**: Clear description of data collection  

## 🚀 Next Steps

1. **Rebuild the app** - BuildConfig changes require rebuild
2. **Test the feature** - Verify toggle works correctly
3. **Update Play Store** - Submit new version
4. **Monitor warning** - Should be resolved after review

## 📝 What Data is Collected (When Enabled)

### ✅ Collected

- App open events
- Photo capture events (TTL duration, whether it has a description)
- Photo actions (save, delete, share)
- Language selection changes
- Feedback dialog interactions

### ❌ NOT Collected

- Photo content or files
- User-entered descriptions
- Personal information
- Location data
- Device ID (when user opts out)

## 🔧 Developer Notes

### To Enable Analytics in Debug Builds (for testing)

In `app/build.gradle.kts`:

```kotlin
debug {
    buildConfigField("boolean", "USE_FIREBASE_ANALYTICS", "true")  // Change to true
}
```

### To Check Current Analytics State

```kotlin
val isEnabled = AnalyticsHelper.isEnabled  // Check if analytics is active
```

### Key Files Modified

- `app/build.gradle.kts` - Build config fields
- `PreferencesManager.kt` - Analytics consent storage
- `AnalyticsHelper.kt` - Consent checking logic
- `SettingsScreen.kt` - UI for Privacy section
- `MainActivity.kt` - Initialize with consent check
- `strings.xml` - User-facing text
- `AndroidManifest.xml` - AD_ID permission comment

## 📖 Full Documentation

See `ANALYTICS_CONSENT_IMPLEMENTATION.md` for complete technical details.

---

**Default Behavior**:

- Debug builds: Analytics OFF (no consent needed)
- Release builds: Analytics ON by default, but user can opt out
- First time users: Analytics enabled (can disable in Settings)
- Opt-out: Takes effect immediately, no app restart needed
