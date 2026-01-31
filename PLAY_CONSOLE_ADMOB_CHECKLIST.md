# Google Play Console Checklist for AdMob

Complete these steps in Google Play Console before publishing your app with ads.

## ✅ Pre-Publishing Checklist

### Step 1: Declare Ads in App Content

- [ ] Go to Play Console: <https://play.google.com/console/>
- [ ] Select your app: "Utility Cam"
- [ ] Navigate to: **Policy** → **App content**
- [ ] Find the "Ads" section
- [ ] Click "Start" (or "Manage" if already started)
- [ ] Select: **"Yes, my app contains ads"**
- [ ] Confirm your selection
- [ ] Click "Save"

**Screenshot Location**: Policy → App content → Ads

### Step 2: Link AdMob Account

- [ ] In Play Console, go to: **Setup** → **API access**
- [ ] Scroll down to "Linked services"
- [ ] Find "Google AdMob"
- [ ] Click "Link" button
- [ ] Follow the prompts to link your AdMob account
- [ ] Confirm the link is established (status shows "Linked")

**Why**: This allows revenue reporting and unified analytics between Play Console and AdMob.

### Step 3: Update Privacy Policy

- [ ] Add ads disclosure to your privacy policy
- [ ] Include the following points:
  - App displays advertisements
  - Uses Google AdMob
  - May collect advertising ID
  - User data shared with ad networks
  - How to opt out of personalized ads
- [ ] Update privacy policy URL in Play Console
- [ ] Go to: **Policy** → **App content** → **Privacy policy**
- [ ] Ensure URL is accessible and up-to-date

**Privacy Policy Template** (add this section):

```
Advertising

Utility Cam displays advertisements in the free version of the app. We use
Google AdMob to serve these ads. AdMob may use your Android Advertising ID
and collect information about your device and app usage to provide relevant
advertisements.

You can opt out of personalized advertising by visiting your device's
settings and disabling "Opt out of Ads Personalization" or through your
Google account settings.

For more information on how Google uses data, please visit:
https://policies.google.com/privacy

Upgrading to Utility Cam Pro removes all advertisements.
```

### Step 4: Update Store Listing (Optional but Recommended)

- [ ] Go to: **Store presence** → **Main store listing**
- [ ] Update app description to mention:
  - "Supported by ads in free version"
  - "Upgrade to Pro for ad-free experience"
- [ ] Example addition:

```
✨ FREE VERSION
- Full-featured photo management
- Ad-supported (banner ads)
- All core features included

🎯 PRO VERSION
- Ad-free experience
- Support ongoing development
- Priority support
- All future Pro features
```

### Step 5: Answer Content Rating Questions

- [ ] Go to: **Policy** → **App content** → **Content rating**
- [ ] If not already completed, fill out IARC questionnaire
- [ ] Ensure you answer "Yes" to:
  - "Does your app contain ads?"
- [ ] Complete and submit rating

### Step 6: Target Audience and Content Settings

- [ ] Go to: **Policy** → **App content** → **Target audience and content**
- [ ] Confirm your target audience (should NOT include children under 13)
- [ ] If targeting children: You MUST comply with COPPA and use family-safe ads only
- [ ] For Utility Cam (photography utility): Likely **not** targeting children

**Important**: Apps targeting children have strict ad requirements!

### Step 7: Review and Submit

- [ ] Review all changes in Play Console
- [ ] Ensure no warnings in the "Policy status" section
- [ ] Create a new release (or update existing)
- [ ] Upload your new APK/AAB with AdMob integration
- [ ] Increment version code
- [ ] Update release notes to mention:
  - "Added ad support for free version"
  - "Upgrade to Pro for ad-free experience"
- [ ] Submit for review

## 📋 Post-Publishing Checklist

### After Your App Is Live

- [ ] **Monitor Ad Performance**
  - Check AdMob Console daily for first week
  - Verify ads are serving (impressions > 0)
  - Check fill rate (should be > 80%)

- [ ] **Check User Reviews**
  - Monitor for complaints about ads
  - Respond to feedback quickly
  - Consider adjustments if many complaints

- [ ] **Track Revenue**
  - Link AdMob to Google Pay for payments
  - Set up payment information in AdMob
  - Track daily earnings

- [ ] **Monitor Analytics**
  - Firebase: Track `ad_loaded`, `ad_load_failed`, `ad_clicked` events
  - Compare Pro upgrade rates before/after ads
  - Track sessions per user (ads shouldn't reduce engagement)

- [ ] **Compliance Check**
  - Ensure no policy violations
  - Monitor AdMob account status
  - Check for warning emails from Google

## ⚠️ Important Warnings

### DO NOT

❌ Click your own ads (instant ban)
❌ Ask users to click ads (violation)
❌ Make ads look like content (deceptive)
❌ Place ads over critical functionality (poor UX)
❌ Refresh ads too frequently (< 30 seconds)

### DO

✅ Use clear ad placements (bottom banners)
✅ Respect user experience
✅ Provide clear Pro upgrade path
✅ Monitor ad performance regularly
✅ Update privacy policy
✅ Comply with all policies

## 🌍 Regional Compliance

### GDPR (Europe)

If you have European users:

- [ ] Consider implementing consent dialog
- [ ] Use Google's UMP (User Messaging Platform)
- [ ] Allow users to opt out of personalized ads
- [ ] Update privacy policy with GDPR-compliant language

### COPPA (USA)

If targeting children under 13:

- [ ] Use family-safe ad settings in AdMob
- [ ] Mark app as "Designed for Families" in Play Console
- [ ] Disable personalized ads for children
- [ ] Additional privacy policy requirements

### Other Regions

- Check local advertising regulations
- Some countries have specific requirements
- Consult legal advice if unsure

## 📞 Support Resources

### If Ads Don't Show

1. Check AdMob Console → Apps → Your App → Status
2. Verify ad units are active
3. Wait 1-2 hours after creating ad units
4. Check device logs for error messages
5. Ensure test device is not blocked

### If Policy Violation

1. Check email from Google Play
2. Review specific violation notice
3. Fix issue immediately
4. Respond through Play Console
5. Appeal if you believe it's a mistake

### Getting Help

- **AdMob Support**: <https://support.google.com/admob/>
- **Play Console Help**: <https://support.google.com/googleplay/android-developer/>
- **Policy Help**: <https://support.google.com/googleplay/android-developer/answer/9857753>

## 📊 Success Metrics

Track these to measure success:

### Week 1

- [ ] Ads are showing (impressions > 0)
- [ ] Fill rate > 80%
- [ ] No policy violations
- [ ] User reviews remain positive (>4.0 stars)

### Month 1

- [ ] Establish baseline eCPM
- [ ] Track Pro conversion rate
- [ ] Compare revenue: Ads vs. Pro purchases
- [ ] Optimize if needed

### Long Term

- [ ] Consistent ad revenue
- [ ] Growing Pro user base
- [ ] High user retention
- [ ] Positive reviews

## 🎯 Your Current Status

Based on the implementation:

✅ **Completed**:

- AdMob SDK integrated
- Banner ads implemented on all screens
- Pro users excluded from ads
- Analytics tracking added
- Test ads working

🔄 **Pending** (Your Action Required):

- Create AdMob account
- Add app to AdMob
- Create ad units
- Update App ID in AndroidManifest.xml
- Update Ad Unit IDs in AdMobBanner.kt
- Complete Play Console steps above
- Update privacy policy
- Submit app update

## 📅 Estimated Timeline

- **AdMob Account Setup**: 15 minutes
- **Play Console Configuration**: 10 minutes
- **Privacy Policy Update**: 15 minutes
- **Testing**: 30 minutes
- **Build & Upload**: 20 minutes
- **Review Process**: 1-3 days (Google's timeline)
- **Ad Activation**: 1-2 hours after approval

**Total Time (Your Work)**: ~1.5 hours
**Total Time (Including Review)**: 1-3 days

## ✅ Final Checklist Before Upload

Before uploading your APK/AAB with ads:

- [ ] Replaced test AdMob App ID with real ID
- [ ] Replaced test Ad Unit IDs with real IDs
- [ ] Privacy policy updated and accessible
- [ ] Tested ads on physical device
- [ ] Verified Pro users don't see ads
- [ ] Incremented version code in build.gradle.kts
- [ ] Updated release notes
- [ ] All Play Console steps completed above
- [ ] Created signed release build
- [ ] Tested release build thoroughly

---

## 🚀 Ready to Launch?

Once all checkboxes are complete:

1. Upload your release to Play Console
2. Submit for review
3. Wait for approval (usually 1-3 days)
4. Monitor closely after going live
5. Celebrate! 🎉

**Need Help?** Refer to `ADMOB_IMPLEMENTATION.md` for detailed guidance.

---

**Document Version**: 1.0
**Last Updated**: January 31, 2026
**App**: Utility Cam v1.6.5
