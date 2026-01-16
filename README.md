# Utility Cam - Android App

A secondary camera app for Android that automatically deletes photos after a configurable time period (24 hours, 3 days, or 1 week).

## Features

### Core Functionality
- **Temporary Photo Storage**: All photos are stored in the app's private sandbox, keeping your main gallery clean
- **Auto-Delete Timer**: Every photo has an expiration timestamp and is automatically deleted when expired
- **Configurable TTL**: Choose between 24 hours (default), 3 days, or 1 week expiration times
- **"Keep Forever" Option**: Save important photos permanently to your device gallery
- **Home Screen Widget**: View active utility photos with expiration countdowns directly on your home screen

### User Interface
- **Modern Jetpack Compose UI**: Built entirely with Jetpack Compose and Material 3
- **Camera Interface**: Full-featured camera with front/back flip capability
- **Photo Review**: Review, add descriptions, and select TTL before saving
- **Gallery View**: Grid view of all active photos with expiration indicators
- **Photo Details**: View full-size photos with detailed information and actions

### Technical Features
- **CameraX Integration**: Modern camera implementation with lifecycle awareness
- **Background Cleanup**: WorkManager-based periodic cleanup (runs every 15 minutes)
- **DataStore Preferences**: Persistent user settings storage
- **Image Thumbnails**: Efficient thumbnail generation with proper EXIF rotation handling
- **Permission Handling**: Proper runtime permission management for camera and storage

## Project Structure

```
utility-cam/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/utility/cam/
│           ├── MainActivity.kt
│           ├── data/
│           │   ├── PhotoStorageManager.kt  # Core storage logic
│           │   ├── PreferencesManager.kt   # Settings management
│           │   └── UtilityPhoto.kt         # Data model
│           ├── ui/
│           │   ├── camera/
│           │   │   └── CameraScreen.kt     # Camera UI
│           │   ├── capturereview/
│           │   │   └── CaptureReviewScreen.kt
│           │   ├── gallery/
│           │   │   └── GalleryScreen.kt    # Main gallery
│           │   ├── photodetail/
│           │   │   └── PhotoDetailScreen.kt
│           │   ├── settings/
│           │   │   └── SettingsScreen.kt
│           │   ├── navigation/
│           │   │   └── Navigation.kt       # Navigation graph
│           │   └── theme/
│           │       ├── Color.kt
│           │       ├── Theme.kt
│           │       └── Type.kt
│           ├── widget/
│           │   └── UtilityCamWidget.kt    # Home screen widget
│           └── worker/
│               └── PhotoCleanupWorker.kt  # Background cleanup
├── build.gradle.kts
└── settings.gradle.kts
```

## How It Works

### Storage Architecture
1. Photos are captured using CameraX and saved to the app's cache directory
2. After review, photos are moved to the app's private files directory (`/data/data/com.utility.cam/files/utility_photos/`)
3. Metadata (including expiration timestamps) is stored in a JSON file
4. Photos never touch the device's main DCIM folder unless explicitly saved

### Expiration Logic
```kotlin
IF (Current_Time > File_Creation_Time + Duration) THEN (Delete_File)
```

The `PhotoCleanupWorker` runs periodically:
- Checks all stored photos against their expiration timestamps
- Deletes expired photos and their thumbnails
- Updates metadata to reflect current state

### The "Keep" Feature
When a user taps "Keep Forever":
1. Photo is copied to the device's DCIM folder using MediaStore API
2. Original file is removed from app's private storage
3. Photo becomes a permanent part of the device gallery

## Building the App

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with minimum API 26 (Android 8.0)
- Target API 34 (Android 14)

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```

### Running the App
1. Open the project in Android Studio
2. Sync Gradle files
3. Connect an Android device or start an emulator
4. Click Run or press Shift+F10

## Key Dependencies

- **Jetpack Compose**: Modern UI toolkit
- **CameraX**: Camera implementation
- **WorkManager**: Background task scheduling
- **Glance**: Widget framework
- **Coil**: Image loading
- **DataStore**: Preferences storage
- **Navigation Compose**: Screen navigation
- **Accompanist Permissions**: Runtime permission handling

## Permissions

The app requests the following permissions:
- `CAMERA` - For capturing photos
- `READ_MEDIA_IMAGES` (API 33+) - For saving to gallery
- `WRITE_EXTERNAL_STORAGE` (API 28 and below) - For saving to gallery
- `POST_NOTIFICATIONS` - For potential future notification features

## Widget

Add the Utility Cam widget to your home screen to see:
- Up to 5 active photos with descriptions
- Countdown timers showing time remaining before deletion
- Quick overview of your temporary photo inventory

## Future Enhancements

Potential features for future versions:
- Video support
- Cloud backup option for "important" photos
- Custom notification when photos are about to expire
- Tags and categories
- Search functionality
- Share photos directly from the app
- Batch operations (delete multiple, save multiple)

## License

This is a sample project for demonstration purposes.
