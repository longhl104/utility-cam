# AdMob Implementation Guide

This document describes how AdMob banner ads have been integrated into Utility Cam and provides instructions for setting up AdMob in Google Play Console and AdMob Console.

## Overview

Banner ads have been added to all screens for non-pro users. Pro users will not see any ads, making this a key incentive for upgrading to Pro.

## Implementation Details

### Files Created

1. **`app/src/main/java/com/utility/cam/ui/ads/AdMobBanner.kt`**
   - Reusable AdMob banner composable
   - `AdMobBanner`: Core banner ad component
   - `BottomAdBanner`: Helper composable that only shows ads for non-pro users
   - `TopAdBanner`: Alternative placement option
   - `AdUnitIds`: Object containing all ad unit IDs

### Files Modified

1. **`app/build.gradle.kts`**
   - Added AdMob dependency: `com.google.android.gms:play-services-ads:23.6.0`

2. **`app/src/main/AndroidManifest.xml`**
   - Added AdMob App ID metadata
   - Currently using test ID: `ca-app-pub-3940256099942544~3347511713`

3. **`app/src/main/java/com/utility/cam/analytics/AnalyticsHelper.kt`**
   - Added analytics tracking for ad events:
     - `logAdLoaded(screenName)` - Ad successfully loaded
     - `logAdLoadFailed(screenName, errorMessage)` - Ad failed to load
     - `logAdClicked(screenName)` - User clicked on ad

4. **Screen Integrations:**
   - `GalleryScreen.kt` - Bottom banner in scaffold
   - `SettingsScreen.kt` - Bottom banner in scaffold
   - `CameraScreen.kt` - Bottom banner in main Box
   - `MediaDetailScreen.kt` - Bottom banner in scaffold
   - `CaptureReviewScreen.kt` - Bottom banner in scaffold
   - `PdfGeneratorScreen.kt` - Bottom banner in scaffold

### Ad Placement Strategy

- **Gallery Screen**: Bottom banner (primary screen)
- **Camera Screen**: Bottom banner (doesn't interfere with camera controls)
- **Settings Screen**: Bottom banner
- **Media Detail Screen**: Bottom banner
- **Capture Review Screen**: Bottom banner
- **PDF Generator Screen**: Bottom banner
- **Pro Screen**: No ads (users are considering purchase)

### Ad Visibility Logic

```kotlin
if (!isProUser) {
    BottomAdBanner(
        isProUser = actualIsProUser,
        screenName = "ScreenName"
    )
}
```

Ads are only shown when:
- User is NOT a Pro user (verified via BillingManager)
- In debug builds, Pro status can be overridden in Settings

## Setup Instructions

### Part 1: Google AdMob Console Setup

#### 1. Create AdMob Account
1. Go to https://admob.google.com/
2. Sign in with your Google account
3. Click "Get Started" if this is your first time
4. Accept the AdMob Terms of Service

#### 2. Link Your App
1. Click "Apps" in the left sidebar
2. Click "Add App"
3. Select "Android"
4. Enter app details:
   - **Is your app listed on a supported app store?**: Yes
   - **Search for your app**: Search for "Utility Cam" or enter package name: `com.utility.cam`
5. Confirm app selection and click "Continue"

#### 3. Get Your AdMob App ID
1. After adding your app, you'll see the App ID in the format: `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`
2. Copy this App ID

#### 4. Create Ad Units

For **each screen**, create a separate banner ad unit:

##### Gallery Screen Ad Unit
1. Go to your app in AdMob Console
2. Click "Ad units" → "Add ad unit"
3. Select "Banner"
4. Configure:
   - **Ad unit name**: "Gallery Screen Banner"
   - **Ad format**: Banner (320x50)
5. Click "Create ad unit"
6. Copy the **Ad unit ID** (format: `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`)

##### Repeat for Each Screen:
- **Camera Screen Banner**
- **Settings Screen Banner**
- **Media Detail Banner**
- **Capture Review Banner**
- **PDF Generator Banner**

You can use the same ad unit ID for all screens or create separate ones for better analytics.

#### 5. Configure Mediation (Optional)
1. Go to "Mediation" in AdMob Console
2. Create mediation groups for better fill rates
3. Add networks like Facebook Audience Network, Unity Ads, etc.

### Part 2: Update Your Android Project

#### 1. Update AndroidManifest.xml
Replace the test App ID with your real AdMob App ID:

```xml
<!-- AdMob App ID -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>
```

**Location**: `app/src/main/AndroidManifest.xml` (around line 62)

#### 2. Update Ad Unit IDs
Edit `app/src/main/java/com/utility/cam/ui/ads/AdMobBanner.kt`:

```kotlin
object AdUnitIds {
    // Replace with your actual AdMob ad unit IDs
    const val BANNER_GALLERY = "ca-app-pub-XXXXXXXXXXXXXXXX/1111111111"
    const val BANNER_CAMERA = "ca-app-pub-XXXXXXXXXXXXXXXX/2222222222"
    const val BANNER_SETTINGS = "ca-app-pub-XXXXXXXXXXXXXXXX/3333333333"
    const val BANNER_MEDIA_DETAIL = "ca-app-pub-XXXXXXXXXXXXXXXX/4444444444"
    const val BANNER_CAPTURE_REVIEW = "ca-app-pub-XXXXXXXXXXXXXXXX/5555555555"
    const val BANNER_PDF_GENERATOR = "ca-app-pub-XXXXXXXXXXXXXXXX/6666666666"
}
```

**Note**: For simplicity, you can use the same ad unit ID for all screens.

#### 3. Use Production Ad Unit IDs Based on Build Type (Optional)

For better development experience, you can keep test IDs in debug builds:

```kotlin
object AdUnitIds {
    private const val DEBUG_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val PROD_BANNER_GALLERY = "ca-app-pub-XXXXXXXXXXXXXXXX/1111111111"

    val BANNER_GALLERY = if (BuildConfig.DEBUG) DEBUG_BANNER_ID else PROD_BANNER_GALLERY
    // ... repeat for other screens
}
```

### Part 3: Google Play Console Setup

#### 1. Enable Ads Declaration
1. Go to https://play.google.com/console/
2. Select your app "Utility Cam"
3. Navigate to "Policy" → "App content"
4. Find "Ads" section
5. Click "Start" or "Manage"
6. Select "Yes, my app contains ads"
7. Answer the questions:
   - **Does your app contain ads?**: Yes
   - **Do you use AdMob?**: Yes
   - **Are ads displayed using Google Play's ad services?**: Yes
8. Save changes

#### 2. Link AdMob Account to Play Console
1. In Play Console, go to "Setup" → "API access"
2. Find "Google AdMob" in the linked services
3. Click "Link" if not already linked
4. This allows revenue sharing and unified reporting

#### 3. Update Privacy Policy
Your privacy policy must mention:
- The app displays ads
- Use of Google AdMob
- Data collection by ad networks
- User's advertising ID usage

Add to your privacy policy:
```
Advertising

We use Google AdMob to display advertisements in the free version of our app.
AdMob may use your Android Advertising ID and collect information about your
device and app usage to serve personalized ads. You can opt out of personalized
ads in your device settings.

For more information on how Google uses data, visit:
https://policies.google.com/privacy
```

#### 4. Update Store Listing (Optional)
Consider updating your app description to mention:
- "Ad-free experience with Pro upgrade"
- "Supports development through ethical advertising"

### Part 4: Testing

#### Before Publishing

1. **Test with Real Ads in Internal Testing**:
   ```kotlin
   // In AdMobBanner.kt, use test IDs for debug builds
   const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
   ```

2. **Add Test Device**:
   ```kotlin
   val testDeviceIds = listOf("YOUR_DEVICE_ID")
   val configuration = RequestConfiguration.Builder()
       .setTestDeviceIds(testDeviceIds)
       .build()
   MobileAds.setRequestConfiguration(configuration)
   ```

3. **Verify Ad Loading**:
   - Check Logcat for ad events
   - Look for "Ad loaded successfully" messages
   - Test on multiple screen sizes

4. **Test Pro Upgrade**:
   - Enable debug Pro override in Settings
   - Verify ads disappear when Pro is enabled
   - Disable override and verify ads reappear

#### After Publishing

1. **Monitor in AdMob Console**:
   - Check impression rates
   - Monitor fill rates
   - Review eCPM (effective cost per mille)

2. **Track Analytics**:
   - Use Firebase Analytics to track:
     - Ad load success/failure rates
     - Ad click-through rates
     - Correlation between ads and Pro upgrades

3. **Adjust Strategy**:
   - If fill rates are low, enable mediation
   - If user complaints increase, review ad placement
   - Monitor if ads drive Pro upgrades

## Revenue Optimization Tips

### 1. Mediation
- Add multiple ad networks to increase fill rate
- Networks to consider:
  - Facebook Audience Network
  - Unity Ads
  - AppLovin
  - ironSource

### 2. Ad Placement Best Practices
- **Current implementation**: Bottom banner (non-intrusive)
- **Avoid**: Interstitial ads (annoying for utility apps)
- **Consider**: Native ads in gallery grid (future enhancement)

### 3. User Experience
- ✅ Ads only for non-Pro users
- ✅ No ads on Pro upgrade screen
- ✅ Clear value proposition for Pro (ad-free)
- ✅ Non-intrusive placement (bottom banners)

### 4. Conversion Tracking
Track these metrics in Firebase Analytics:
- Ad impressions per session
- Time to Pro upgrade after seeing ads
- Ad-to-Pro conversion rate

## Troubleshooting

### Ads Not Showing

1. **Check AdMob App ID**:
   - Verify in AndroidManifest.xml
   - Must match your AdMob Console App ID

2. **Check Ad Unit IDs**:
   - Verify in AdMobBanner.kt
   - Must match Ad Units created in AdMob Console

3. **Check Internet Permission**:
   - Already added in AndroidManifest.xml

4. **Wait for Ad Serving**:
   - New ad units take 1-2 hours to activate
   - First impressions may take longer

5. **Check Logs**:
   - Filter Logcat by "AdMobBanner"
   - Look for error messages

### Low Fill Rate

1. **Enable Mediation**:
   - Add multiple ad networks
   - Configure waterfall or bidding

2. **Check Country Support**:
   - Some regions have lower fill rates
   - Consider regional mediation partners

3. **Review Ad Requests**:
   - Too frequent requests may be throttled
   - Ensure proper ad lifecycle management

### Policy Violations

1. **Avoid**:
   - Click fraud (encouraging users to click ads)
   - Ad stacking (multiple ads overlapping)
   - Misleading placement (ads look like content)

2. **Follow**:
   - Google AdMob policies
   - Google Play policies
   - Local advertising regulations (GDPR, COPPA, etc.)

## GDPR & Privacy Compliance

### User Consent (EU Users)

Consider implementing Google's UMP (User Messaging Platform):

```kotlin
// Add dependency
implementation 'com.google.android.ump:user-messaging-platform:2.2.0'

// Request consent
val consentInformation = UserMessagingPlatform.getConsentInformation(context)
val params = ConsentRequestParameters.Builder().build()
consentInformation.requestConsentInfoUpdate(activity, params,
    { /* Load form if needed */ },
    { /* Handle error */ }
)
```

### Advertising ID Permission

Already added in AndroidManifest.xml:
```xml
<uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="replace" />
```

Users can opt out of personalized ads in device settings.

## Analytics & Monitoring

### Key Metrics to Track

1. **Ad Performance**:
   - Impressions per user
   - Click-through rate (CTR)
   - eCPM (earnings per 1000 impressions)

2. **User Behavior**:
   - Time between app install and first ad view
   - Sessions before Pro upgrade
   - Ad-to-Pro conversion rate

3. **Technical Metrics**:
   - Ad load success rate
   - Ad load failure reasons
   - Average ad load time

### Firebase Analytics Events

Already implemented:
```kotlin
AnalyticsHelper.logAdLoaded(screenName)
AnalyticsHelper.logAdLoadFailed(screenName, errorMessage)
AnalyticsHelper.logAdClicked(screenName)
```

View in Firebase Console → Analytics → Events.

## Future Enhancements

### Potential Improvements

1. **Native Ads in Gallery**:
   - Show ads within photo grid
   - Better user experience
   - Higher engagement

2. **Rewarded Ads**:
   - "Watch ad to extend photo expiration"
   - "Watch ad to unlock temporary Pro features"

3. **Adaptive Banners**:
   - Use full screen width
   - Better for tablets

4. **Smart Frequency Capping**:
   - Limit ad impressions per user
   - Reduce ad fatigue

5. **A/B Testing**:
   - Test different ad placements
   - Measure impact on Pro conversions

## Summary Checklist

### Before Publishing
- [ ] Create AdMob account
- [ ] Add your app to AdMob
- [ ] Create ad units for each screen
- [ ] Replace test App ID in AndroidManifest.xml
- [ ] Replace test Ad Unit IDs in AdMobBanner.kt
- [ ] Update privacy policy
- [ ] Test ads with real Ad Unit IDs
- [ ] Verify Pro users don't see ads
- [ ] Enable ads declaration in Play Console
- [ ] Link AdMob account to Play Console

### After Publishing
- [ ] Monitor ad impressions in AdMob Console
- [ ] Track ad analytics in Firebase Console
- [ ] Monitor user reviews for ad complaints
- [ ] Check fill rates and eCPM
- [ ] Consider enabling mediation if fill rate is low
- [ ] Track Pro conversion rate
- [ ] Adjust strategy based on data

## Support & Resources

- **AdMob Help**: https://support.google.com/admob/
- **AdMob Policies**: https://support.google.com/admob/answer/6128543
- **Google Play Policies**: https://support.google.com/googleplay/android-developer/answer/9857753
- **Firebase Analytics**: https://firebase.google.com/docs/analytics
- **Ad Network Mediation**: https://support.google.com/admob/answer/3063564

## Contact

For questions about this implementation, contact the development team.

---

**Last Updated**: January 31, 2026
**Implementation Version**: 1.0
**App Version**: 1.6.5
