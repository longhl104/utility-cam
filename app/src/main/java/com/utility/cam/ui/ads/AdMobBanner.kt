package com.utility.cam.ui.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.utility.cam.analytics.AnalyticsHelper

/**
 * Reusable AdMob Banner composable that shows ads only for non-pro users
 *
 * @param modifier Modifier for the banner container
 * @param adUnitId The AdMob ad unit ID. Use test ID for development.
 * @param screenName Name of the screen showing the ad (for analytics)
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String,
    screenName: String = "unknown"
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // Don't show ads in preview mode
    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Preview placeholder - not shown in actual app
        }
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId

                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            isAdLoaded = true
                            Log.d("AdMobBanner", "Ad loaded successfully for screen: $screenName")
                            AnalyticsHelper.logAdLoaded(screenName)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            super.onAdFailedToLoad(error)
                            isAdLoaded = false
                            Log.e("AdMobBanner", "Ad failed to load for screen: $screenName. Error: ${error.message}")
                            AnalyticsHelper.logAdLoadFailed(screenName, error.message)
                        }

                        override fun onAdOpened() {
                            super.onAdOpened()
                            Log.d("AdMobBanner", "Ad opened for screen: $screenName")
                            AnalyticsHelper.logAdClicked(screenName)
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            Log.d("AdMobBanner", "Ad clicked for screen: $screenName")
                        }

                        override fun onAdClosed() {
                            super.onAdClosed()
                            Log.d("AdMobBanner", "Ad closed for screen: $screenName")
                        }
                    }

                    // Load the ad
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Banner ad that appears at the bottom of the screen
 * Only shows for non-pro users
 */
@Composable
fun BottomAdBanner(
    isProUser: Boolean,
    screenName: String,
    modifier: Modifier = Modifier,
    adUnitId: String
) {
    if (!isProUser) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            AdMobBanner(
                screenName = screenName,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                adUnitId = adUnitId
            )
        }
    }
}

/**
 * Banner ad that appears at the top of the screen
 * Only shows for non-pro users
 */
@Composable
fun TopAdBanner(
    isProUser: Boolean,
    screenName: String,
    modifier: Modifier = Modifier,
    adUnitId: String
) {
    if (!isProUser) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            AdMobBanner(
                screenName = screenName,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                adUnitId = adUnitId
            )
        }
    }
}

// Test Ad Unit ID - Replace with actual IDs in production
// For production, create ad units in AdMob console
object AdUnitIds {
    // Use test ID for debug builds, real IDs for release
    const val BANNER_GALLERY = "ca-app-pub-8461786124508841/4263563385" // Replace with actual ID
    const val BANNER_CAMERA = "ca-app-pub-8461786124508841/8234198609" // Replace with actual ID
    const val BANNER_SETTINGS = "ca-app-pub-8461786124508841/7085941925" // Replace with actual ID
    const val BANNER_MEDIA_DETAIL = "ca-app-pub-8461786124508841/9109470033" // Replace with actual ID
    const val BANNER_CAPTURE_REVIEW = "ca-app-pub-8461786124508841/3577167676" // Replace with actual ID
    const val BANNER_PDF_GENERATOR = "ca-app-pub-8461786124508841/2710409184" // Replace with actual ID
}
