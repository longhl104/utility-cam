# 🔥 Firebase Google Analytics Integration - Setup Guide

## Overview

Firebase Analytics has been successfully integrated into Utility Cam. This guide will help you complete the setup by configuring your Firebase project.

## Prerequisites

- Google account
- Utility Cam app source code
- Firebase Console access

## Step-by-Step Setup

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: **"Utility Cam"** or **"utility-cam"**
4. Click **Continue**
5. (Optional) Enable/Disable Google Analytics (recommended: Enable)
6. Click **Continue**
7. Accept terms and click **Create project**
8. Wait for project creation (30-60 seconds)
9. Click **Continue** when ready

### Step 2: Add Android App to Firebase

1. In Firebase Console, click the **Android icon** or **"Add app"**
2. Enter Android package name: **`com.utility.cam`** ⚠️ MUST MATCH EXACTLY
3. (Optional) Enter app nickname: **"Utility Cam"**
4. (Optional) Enter SHA-1 certificate fingerprint (for debug/release)
5. Click **"Register app"**

### Step 3: Download google-services.json

1. Firebase will generate **`google-services.json`** file
2. Click **"Download google-services.json"**
3. **Important**: Save this file to:
   ```
   D:\Projects\utility-cam\app\google-services.json
   ```
4. ⚠️ **Replace the template file** (`google-services.json.template`) with the real one
5. Click **Continue**

### Step 4: Verify Configuration

1. Open the downloaded `google-services.json` file
2. Verify it contains:
   - `"project_id"`: Your actual project ID
   - `"mobilesdk_app_id"`: Starts with `1:`
   - `"api_key"`: Your actual API key
   - `"package_name"`: `"com.utility.cam"`

Example structure:
```json
{
  "project_info": {
    "project_number": "123456789012",
    "project_id": "utility-cam-abc123",
    "storage_bucket": "utility-cam-abc123.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789012:android:abcdef123456",
        "android_client_info": {
          "package_name": "com.utility.cam"
        }
      },
      "api_key": [
        {
          "current_key": "AIzaSyA_REAL_API_KEY_HERE"
        }
      ]
    }
  ]
}
```

### Step 5: Build and Test

1. Build the app:
   ```powershell
   cd D:\Projects\utility-cam
   .\gradlew assembleDebug
   ```

2. Install and run the app on a device/emulator

3. Open Firebase Console → Analytics → Events
4. Wait 24 hours for first data to appear (or enable Debug View)

### Step 6: Enable Debug View (Optional)

For immediate event verification:

1. Connect your Android device via ADB
2. Run:
   ```powershell
   adb shell setprop debug.firebase.analytics.app com.utility.cam
   ```
3. Run the app
4. Go to Firebase Console → Analytics → DebugView
5. See events in real-time!

To disable debug mode:
```powershell
adb shell setprop debug.firebase.analytics.app .none.
```

## Getting SHA-1 Certificate (Optional)

### For Debug Certificate:

```powershell
cd C:\Users\YOUR_USERNAME\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### For Release Certificate:

```powershell
cd D:\Projects\utility-cam
keytool -list -v -keystore utility-cam-release.keystore -alias utility-cam
```

Copy the **SHA-1** value and add it to Firebase Console:
Project Settings → Your Apps → SHA certificate fingerprints

## File Locations

### Required Files:

- ✅ `app/google-services.json` - **REQUIRED** (Download from Firebase)
- ✅ `build.gradle.kts` (root) - Already configured ✅
- ✅ `app/build.gradle.kts` - Already configured ✅
- ✅ `AnalyticsHelper.kt` - Already created ✅

### Template File (Remove After Setup):

- ❌ `app/google-services.json.template` - DELETE after getting real file

## Troubleshooting

### Issue: "google-services.json is missing"

**Solution**: Download from Firebase Console and place in `app/` folder

### Issue: "Package name mismatch"

**Solution**: Ensure Firebase project uses `com.utility.cam` exactly

### Issue: "No events showing in Firebase"

**Solution**: 
1. Wait 24 hours for data processing
2. Or enable DebugView for real-time events
3. Check internet connection on device

### Issue: "Build failed with Google Services plugin"

**Solution**: Verify `google-services.json` is valid JSON and in correct location

## Security Notes

### ⚠️ IMPORTANT - .gitignore

The `google-services.json` file contains API keys. Ensure it's in `.gitignore`:

```gitignore
# Firebase
app/google-services.json
google-services.json
```

The template file is safe to commit, but the real file should never be committed to Git.

## What's Already Configured

### ✅ Dependencies Added:

- Firebase BOM (Bill of Materials)
- Firebase Analytics KTX
- Google Services Plugin

### ✅ Analytics Events Implemented:

- App launches
- Photo captures (with TTL and description tracking)
- Photo deletions
- Photo shares
- Photos saved to gallery
- Photos auto-cleaned
- Settings changes
- Language changes
- Feedback dialog actions
- Screen views
- Notification settings
- Widget interactions
- Camera features usage

### ✅ Integration Points:

- MainActivity - App launches
- PhotoStorageManager - Photo lifecycle events
- PhotoDetailScreen - Share tracking
- SettingsScreen - Settings changes
- GalleryScreen - Feedback tracking
- FeedbackManager - User engagement

## Next Steps After Setup

1. ✅ Download `google-services.json` from Firebase Console
2. ✅ Place file in `app/` folder
3. ✅ Build and run app
4. ✅ Enable DebugView to see events immediately
5. ✅ Monitor analytics in Firebase Console

## Summary

Your Utility Cam app is ready for Firebase Analytics! Just complete the Firebase Console setup and download the `google-services.json` file. Once that's in place, the app will automatically start tracking all configured events.

**Status**: 🔧 Awaiting google-services.json configuration
