# In-App Review Implementation Guide

## Overview

The `InAppReviewManager` is a reusable class that handles Google Play In-App Review API integration. It follows Google's best practices and provides a clean, consistent way to request app reviews across your application.

## Location

- **Manager Class**: `app/src/main/java/com/utility/cam/data/InAppReviewManager.kt`
- **Dependencies**: Already included in `app/build.gradle.kts`
  - `com.google.android.play:review:2.0.2`
  - `com.google.android.play:review-ktx:2.0.2`

## Usage

### Basic Usage

```kotlin
import com.utility.cam.data.InAppReviewManager

@Composable
fun YourScreen() {
    val context = LocalContext.current
    val inAppReviewManager = remember { InAppReviewManager(context) }

    Button(onClick = {
        val activity = context as? Activity
        if (activity != null) {
            inAppReviewManager.launchReviewFlow(
                activity = activity,
                onComplete = {
                    // Review flow finished (doesn't mean user reviewed)
                    Log.d("YourScreen", "Review flow completed")
                },
                onFallback = {
                    // In-app review not available, open Play Store
                    inAppReviewManager.openPlayStoreForReview()
                }
            )
        }
    }) {
        Text("Rate App")
    }
}
```

### Advanced Usage with Pre-caching

For better performance, you can pre-cache the `ReviewInfo` object ahead of time:

```kotlin
@Composable
fun YourScreen() {
    val context = LocalContext.current
    val inAppReviewManager = remember { InAppReviewManager(context) }

    // Pre-cache when user is likely to trigger review
    LaunchedEffect(Unit) {
        inAppReviewManager.preCacheReviewInfo(
            onSuccess = {
                Log.d("YourScreen", "Review info cached")
            },
            onFailure = { exception ->
                Log.e("YourScreen", "Failed to cache review info", exception)
            }
        )
    }

    // Later, launch the flow
    Button(onClick = {
        val activity = context as? Activity
        if (activity != null) {
            inAppReviewManager.launchReviewFlow(activity)
        }
    }) {
        Text("Rate App")
    }
}
```

## Current Implementation

The `InAppReviewManager` is currently used in:

1. **SettingsScreen** - "Love Utility Cam?" button (manual trigger)
2. **FeedbackDialog** - "Rate Now" button (manual trigger)
3. **GalleryScreen** - Automatic trigger after saving 5 photos to gallery
4. **MediaDetailScreen** - Automatic trigger after saving photos to gallery

### Automatic Review Triggers

The app intelligently triggers in-app reviews when users demonstrate value by saving photos:

- **Threshold**: After saving 5 photos total
- **Cooldown**: Won't trigger more than once every 30 days
- **Graceful**: If user has already rated, won't trigger again
- **Silent fallback**: If review API is unavailable, it silently skips (no Play Store popup)

This is managed by `FeedbackManager.shouldTriggerReviewAfterSave()`.

## Important Notes

### When In-App Review Works

- ✅ **Production builds** on real devices from Play Store
- ✅ **Internal testing** builds distributed through Play Console
- ❌ **Debug builds** (API will silently fail)
- ❌ **Emulators** (may not work consistently)
- ❌ **Side-loaded APKs** (not from Play Store)

### Google's Quotas

Google limits how often the in-app review dialog can be shown to users:
- The API may not show the dialog even when called successfully
- This is intentional to prevent review spam
- Always provide a fallback (Play Store link) for users who want to leave a review

### Best Practices

1. **Don't ask immediately**: Wait until users have experienced value from your app
2. **Don't ask repeatedly**: Respect the user's decision if they dismiss the review prompt
3. **Don't interrupt critical flows**: Ask at natural break points (e.g., after completing a task)
4. **Always provide fallback**: Use `openPlayStoreForReview()` as a fallback option

## API Reference

### `InAppReviewManager(context: Context)`

Constructor that creates a new instance of the manager.

### `preCacheReviewInfo(onSuccess: () -> Unit, onFailure: (Exception) -> Unit)`

Pre-cache the ReviewInfo object. Call this before you're ready to show the review flow.

**Note**: The ReviewInfo object is only valid for a limited time, so only pre-cache when you're certain the app will launch the review flow soon.

### `launchReviewFlow(activity: Activity, onComplete: () -> Unit, onFallback: () -> Unit)`

Launch the in-app review flow.

- **activity**: Required Activity context to launch the flow
- **onComplete**: Called when flow finishes (doesn't indicate if user actually reviewed)
- **onFallback**: Called if the API is not available or fails

If ReviewInfo is not cached, this will request it on-demand.

### `openPlayStoreForReview()`

Opens the app's page in Google Play Store as a fallback. Tries to open in the Play Store app first, then falls back to browser if the app is not installed.

## Testing

To test the in-app review flow:

1. Build and upload to Play Console (Internal Testing track)
2. Download the app from Play Store
3. Trigger the review flow
4. The dialog should appear (subject to Google's quotas)

For development, the `onFallback` callback will be triggered, opening the Play Store instead.

## Troubleshooting

**Issue**: Review dialog doesn't appear
- **Solution**: This is expected in debug builds and on emulators. Test with an internal testing build from Play Console.

**Issue**: "Opening Play Store..." appears every time
- **Solution**: The in-app review API has quotas. If you've triggered it many times, wait a while or test with a fresh Play Store account.

**Issue**: App crashes when clicking review button
- **Solution**: Make sure you're passing an Activity context, not just a regular Context.
