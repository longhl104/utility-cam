package com.utility.cam.ui.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.utility.cam.analytics.AnalyticsHelper
import android.widget.ImageView
import android.widget.TextView
import android.view.LayoutInflater
import com.utility.cam.R

/**
 * Native Advanced Ad that appears inline with content
 * Designed to blend naturally with the photo/video grid
 */
@Composable
fun NativeAdvancedAd(
    modifier: Modifier = Modifier,
    adUnitId: String,
    screenName: String = "unknown",
    isProUser: Boolean = false
) {
    // Don't show ads for Pro users
    if (isProUser) {
        return
    }

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    // Don't show ads in preview mode
    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Native Ad Placeholder", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    // Load native ad
    LaunchedEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                // Clean up old ad
                nativeAd?.destroy()
                nativeAd = ad
                isAdLoaded = true
                Log.d("NativeAdvancedAd", "Native ad loaded successfully for screen: $screenName")
                AnalyticsHelper.logAdLoaded(screenName)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("NativeAdvancedAd", "Failed to load native ad for screen: $screenName. Error: ${error.message}")
                    AnalyticsHelper.logAdLoadFailed(screenName, error.message)
                    isAdLoaded = false
                }

                override fun onAdClicked() {
                    Log.d("NativeAdvancedAd", "Native ad clicked for screen: $screenName")
                    AnalyticsHelper.logAdClicked(screenName)
                }

                override fun onAdOpened() {
                    Log.d("NativeAdvancedAd", "Native ad opened for screen: $screenName")
                }

                override fun onAdClosed() {
                    Log.d("NativeAdvancedAd", "Native ad closed for screen: $screenName")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setRequestMultipleImages(false)
                    .setVideoOptions(
                        com.google.android.gms.ads.VideoOptions.Builder()
                            .setStartMuted(true)
                            .build()
                    )
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    // Show native ad if loaded
    if (isAdLoaded && nativeAd != null) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            factory = { ctx ->
                val inflater = LayoutInflater.from(ctx)
                val adView = inflater.inflate(R.layout.native_ad_grid_item, null) as NativeAdView

                // Populate the ad view with native ad assets
                nativeAd?.let { ad ->
                    // Set ad view components
                    adView.headlineView = adView.findViewById(R.id.ad_headline)
                    adView.bodyView = adView.findViewById(R.id.ad_body)
                    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                    adView.iconView = adView.findViewById(R.id.ad_icon)
                    adView.mediaView = adView.findViewById(R.id.ad_media)

                    // Populate headline
                    ad.headline?.let { headline ->
                        (adView.headlineView as? TextView)?.text = headline
                    }

                    // Populate body
                    ad.body?.let { body ->
                        (adView.bodyView as? TextView)?.text = body
                        adView.bodyView?.visibility = android.view.View.VISIBLE
                    } ?: run {
                        adView.bodyView?.visibility = android.view.View.GONE
                    }

                    // Populate call to action
                    ad.callToAction?.let { cta ->
                        (adView.callToActionView as? TextView)?.text = cta
                        adView.callToActionView?.visibility = android.view.View.VISIBLE
                    } ?: run {
                        adView.callToActionView?.visibility = android.view.View.INVISIBLE
                    }

                    // Populate icon
                    ad.icon?.let { icon ->
                        (adView.iconView as? ImageView)?.setImageDrawable(icon.drawable)
                        adView.iconView?.visibility = android.view.View.VISIBLE
                    } ?: run {
                        adView.iconView?.visibility = android.view.View.GONE
                    }

                    // Populate media (images/videos)
                    adView.mediaView?.let { mediaView ->
                        ad.mediaContent?.let { mediaContent ->
                            mediaView.mediaContent = mediaContent
                        }
                    }

                    // Register the ad view with the native ad
                    adView.setNativeAd(ad)
                }

                adView
            }
        )
    }
}

/**
 * Native Ad Unit IDs for different screens
 */
object NativeAdUnitIds {
    const val GALLERY_NATIVE = "ca-app-pub-8461786124508841/3084446724"
    const val BIN_NATIVE = "ca-app-pub-8461786124508841/4609620694"
}
