# Google Play In-App Review Implementation

## Overview

The app now uses Google Play's In-App Review API to request reviews gracefully, only when users have experienced value from the app.

## How It Works

### 1. User Engagement Tracking

`FeedbackManager.kt` tracks user engagement:

- **Photo count**: Number of photos captured
- **App launches**: Number of times the app was opened
- **Last prompt time**: When the user was last asked for feedback
- **Dismissal count**: How many times the user dismissed the prompt
- **Has rated**: Whether the user has already rated the app

### 2. Smart Triggering Conditions

The review prompt is shown when **ALL** conditions are met:

- ✅ User has captured **at least 3 photos** (MIN_PHOTOS_TAKEN)
- ✅ User has launched the app **at least 5 times** (MIN_APP_LAUNCHES)
- ✅ User hasn't rated the app yet
- ✅ User hasn't dismissed the prompt more than **2 times** (MAX_DISMISSALS)
- ✅ At least **7 days** have passed since the last prompt (DAYS_BETWEEN_PROMPTS)

### 3. Graceful User Experience

**When users have seen value:**

1. A friendly dialog appears: "Enjoying Utility Cam?"
2. User can choose:
   - **Rate Now**: Triggers In-App Review API
   - **Maybe Later**: Dismisses for now, will ask again after 7 days
   - **No Thanks**: Close dialog (counts as dismissal)

**In-App Review API Flow:**

1. Attempts to show Google's native review dialog (seamless)
2. If successful: User rates without leaving the app
3. If API unavailable: Falls back to opening Play Store
4. User experience is smooth and non-intrusive

### 4. Where It's Triggered

**GalleryScreen.kt** (Main screen):

- Checks conditions after 3 seconds (screen has settled)
- Shows FeedbackDialog if conditions are met
- Automatically tracks that prompt was shown

### 5. Google Play In-App Review Behavior

**Important Notes:**

- Google controls when/how often the dialog actually appears
- Has internal quotas to prevent spam
- **Does NOT work in all environments** ⚠️
- When quota is exceeded, silently fails (by design)
- Automatic fallback to Play Store when it fails

### 6. Environmental Limitations ⚠️

The In-App Review API has strict requirements and **will NOT work** in these environments:

#### Does NOT Work

- ❌ **Debug builds** - API is disabled, always falls back to Play Store
- ❌ **Emulators/Simulators** - Requires real device
- ❌ **Internal testing** - Must be published to production
- ❌ **Sideloaded APKs** - Must be installed from Play Store
- ❌ **Quota exceeded** - Google limits frequency per user
- ❌ **First install** - Rarely works immediately after install
- ❌ **Developer accounts** - Often disabled for app developers

#### DOES Work

- ✅ **Production builds** from Play Store
- ✅ **Real devices** (not emulators)
- ✅ **Published apps** on Play Store
- ✅ **Regular users** (not developers)
- ✅ **After app usage** (not immediately after install)
- ✅ **Within quota limits** (not asked too frequently)

#### Fallback Strategy

Our implementation handles all failure cases:

1. **SettingsScreen button**: Shows toast and opens Play Store
2. **FeedbackDialog**: Automatically opens Play Store on failure
3. **Debug builds**: Explicitly opens Play Store after In-App Review attempt
4. **User feedback**: Toast notification when opening Play Store

### 7. Testing

**Manual Testing:**

- Use "Love Utility Cam?" button in Settings
- Instantly triggers In-App Review API (no conditions)
- Check logs for "SettingsScreen" tag

**Automatic Trigger Testing:**

- Reset feedback state: Settings → Debug Tools → "Reset Feedback State"
- Manually trigger conditions by:
  - Taking 3+ photos
  - Launching app 5+ times
- Check logs for "FeedbackDialog" tag

**Note:** In-App Review API rarely works in debug builds. For production testing:

1. Build a release APK
2. Install via internal testing track
3. Use manual button OR meet automatic conditions
4. Review dialog should appear

## Files Modified

### 1. `FeedbackDialog.kt`

- Added Google Play In-App Review API integration
- Attempts In-App Review first
- Falls back to Play Store if API fails
- Proper error handling and logging

### 2. `SettingsScreen.kt`

- Kept "Love Utility Cam?" button for manual review requests
- Button triggers In-App Review API immediately (no conditions)
- Falls back to Play Store if API unavailable
- Marks user as rated when completed

### 3. `build.gradle.kts`

- Added dependencies:

  ```kotlin
  implementation("com.google.android.play:review:2.0.2")
  implementation("com.google.android.play:review-ktx:2.0.2")
  ```

## Benefits

✅ **Non-intrusive**: Only asks when users have used the app (automatic)
✅ **Smart timing**: After user has seen value (3+ photos, 5+ launches)
✅ **Respectful**: Won't spam users (max 2 dismissals, 7-day gaps)
✅ **Seamless**: In-app review keeps users in the app
✅ **Dual approach**: Automatic for natural flow + Manual for eager users
✅ **Fallback**: Opens Play Store if API unavailable
✅ **Production-ready**: Proper error handling and logging

## Customization

To adjust thresholds, edit `FeedbackManager.kt`:

```kotlin
private const val MIN_PHOTOS_TAKEN = 3      // Change minimum photos
private const val MIN_APP_LAUNCHES = 5      // Change minimum launches
private const val DAYS_BETWEEN_PROMPTS = 7  // Change days between prompts
private const val MAX_DISMISSALS = 2        // Change max dismissals
```

## Analytics

The system automatically tracks:

- When feedback prompt is shown
- User's choice (Rate Now / Maybe Later)
- Number of dismissals
- Whether user has rated

## References

- [Google Play In-App Review API](https://developer.android.com/guide/playcore/in-app-review)
- [Best Practices for In-App Reviews](https://developer.android.com/guide/playcore/in-app-review#when-to-request)
