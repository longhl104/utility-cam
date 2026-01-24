# Save-Based In-App Review Trigger Implementation

## Overview

Implemented automatic in-app review triggers that appear after users save a certain number of photos to their gallery. This follows Google's best practice of asking for reviews when users have experienced value from the app.

## Implementation Details

### Changes Made

#### 1. FeedbackManager Updates (`data/FeedbackManager.kt`)

Added tracking for saved photos:

- **New DataStore Keys**:
  - `SAVED_PHOTOS_COUNT_KEY` - Tracks cumulative saved photos
  - `LAST_SAVE_REVIEW_TIME_KEY` - Tracks last time save-based review was triggered

- **New Constants**:
  - `PHOTOS_SAVED_FOR_REVIEW = 5` - Trigger after 5 photos saved
  - `DAYS_BETWEEN_SAVE_REVIEWS = 30` - 30-day cooldown between triggers

- **New Methods**:

  ```kotlin
  suspend fun incrementSavedPhotoCount(count: Int = 1)
  suspend fun shouldTriggerReviewAfterSave(): Boolean
  ```

#### 2. GalleryScreen Updates (`ui/gallery/GalleryScreen.kt`)

- Added `InAppReviewManager` instance
- Updated batch save confirmation dialog to:
  - Track saved photos with `feedbackManager.incrementSavedPhotoCount()`
  - Check if review should trigger with `feedbackManager.shouldTriggerReviewAfterSave()`
  - Launch review flow when threshold is met
  - Mark user as rated on completion

#### 3. MediaDetailScreen Updates (`ui/mediadetail/MediaDetailScreen.kt`)

- Added `FeedbackManager` and `InAppReviewManager` instances
- Updated single save confirmation dialog with same logic as GalleryScreen
- Ensures review can trigger from both batch and single saves

## User Experience Flow

1. **User saves photos** (either single or batch)
2. **Counter increments** silently in background
3. **After 5th saved photo**:
   - Small 1-second delay to let UI settle
   - In-app review dialog appears (if available)
   - User can rate or dismiss
4. **Cooldown activates**:
   - Won't trigger again for 30 days
   - Won't trigger if user already rated
   - Counter resets for next cycle

## Smart Behavior

### When Review Triggers

✅ User has saved at least 5 photos
✅ User hasn't rated before
✅ At least 30 days since last save-based review (or first time)
✅ Review API is available (production/internal testing builds)

### When Review Doesn't Trigger

❌ User has already rated
❌ Less than 5 photos saved
❌ Triggered within last 30 days
❌ Review API unavailable (debug builds, emulators) - fails silently

### Graceful Fallback

- If in-app review API is unavailable, it **silently skips**
- No Play Store popup (only for manual "Love Utility Cam?" button)
- No error messages to user
- Preserves seamless UX

## Configuration

All thresholds can be adjusted in `FeedbackManager.kt`:

```kotlin
companion object {
    // ... existing code ...

    // Thresholds for in-app review after saving photos
    private const val PHOTOS_SAVED_FOR_REVIEW = 5
    private const val DAYS_BETWEEN_SAVE_REVIEWS = 30
}
```

### Recommended Values

- **Conservative** (current): 5 photos, 30 days
- **Aggressive**: 3 photos, 14 days
- **Very Aggressive**: 2 photos, 7 days

## Testing

### Test the Feature

1. **Clear app data** to reset counters
2. **Capture and save 5 photos** (or your threshold)
3. **On 5th save**, review dialog should appear (if in production/internal testing build)

### Debug Mode

In debug builds, the review won't show but logs will indicate:

```
GalleryScreen: Triggering in-app review after saving X photos
GalleryScreen: Review flow not available, skipping
```

### Reset Testing

To reset and test again:

```kotlin
// In debug settings or manually
coroutineScope.launch {
    feedbackManager.resetFeedbackState()
}
```

## Analytics Integration

The implementation logs events for tracking:

```kotlin
AnalyticsHelper.logBatchSave(savedCount) // Already exists
// Review completion tracked in onComplete callback
```

Consider adding specific analytics for review triggers:

```kotlin
AnalyticsHelper.logReviewTriggered("save_based", savedCount)
```

## Best Practices Followed

✅ **Timing**: Trigger after user experiences value (saving photos)
✅ **Frequency**: Reasonable cooldown (30 days)
✅ **Respect**: Don't ask if already rated
✅ **Non-intrusive**: Small delay, graceful failures
✅ **Quota-aware**: Google limits review prompts automatically
✅ **Fallback**: Silent failure in unavailable environments

## Future Enhancements

1. **A/B Testing**: Test different thresholds (3 vs 5 vs 7 photos)
2. **Analytics Dashboard**: Track review trigger success rates
3. **User Segments**: Different thresholds for new vs returning users
4. **Multi-criteria**: Combine saved photos with app usage duration
5. **Smart Timing**: Avoid triggering during busy workflows

## Documentation

- Main implementation: `IN_APP_REVIEW_USAGE.md`
- This feature guide: `SAVE_BASED_REVIEW_TRIGGER.md`

## Related Files

- `app/src/main/java/com/utility/cam/data/FeedbackManager.kt`
- `app/src/main/java/com/utility/cam/data/InAppReviewManager.kt`
- `app/src/main/java/com/utility/cam/ui/gallery/GalleryScreen.kt`
- `app/src/main/java/com/utility/cam/ui/mediadetail/MediaDetailScreen.kt`
