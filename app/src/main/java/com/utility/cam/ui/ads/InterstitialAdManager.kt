package com.utility.cam.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.utility.cam.analytics.AnalyticsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for interstitial ads with smart frequency capping
 * Shows full-screen ads at natural transition points
 */
class InterstitialAdManager(
    private val context: Context,
    private val adUnitId: String = InterstitialAdUnitIds.GENERAL
) {
    companion object {
        private const val TAG = "InterstitialAdManager"

        // Frequency capping: minimum time between ads (in milliseconds)
        private const val MIN_AD_INTERVAL = 60_000L // 1 minute

        // Counter-based capping: show ad every N actions
        private const val ACTIONS_BETWEEN_ADS = 3
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastAdShownTime = 0L
    private var actionsSinceLastAd = 0

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isAdShowing = MutableStateFlow(false)
    val isAdShowing: StateFlow<Boolean> = _isAdShowing.asStateFlow()

    init {
        loadAd()
    }

    /**
     * Load an interstitial ad in the background
     */
    private fun loadAd() {
        if (isLoading || interstitialAd != null) {
            Log.d(TAG, "Ad already loaded or loading")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoading = false
                    _isAdReady.value = true
                    AnalyticsHelper.logAdLoaded("Interstitial")

                    // Set up callbacks for when ad is shown
                    setupAdCallbacks(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: ${loadAdError.message}")
                    interstitialAd = null
                    isLoading = false
                    _isAdReady.value = false
                    AnalyticsHelper.logAdLoadFailed("Interstitial", loadAdError.message)

                    // Retry loading after a delay (optional)
                    // Handler(Looper.getMainLooper()).postDelayed({ loadAd() }, 30000)
                }
            }
        )
    }

    private fun setupAdCallbacks(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                _isAdShowing.value = false
                interstitialAd = null
                _isAdReady.value = false
                lastAdShownTime = System.currentTimeMillis()

                // Load next ad
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                _isAdShowing.value = false
                interstitialAd = null
                _isAdReady.value = false

                // Load next ad
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed full screen content")
                _isAdShowing.value = true
                AnalyticsHelper.logAdClicked("Interstitial")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Ad was clicked")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Ad impression recorded")
            }
        }
    }

    /**
     * Check if enough time has passed since last ad
     */
    private fun hasEnoughTimePassed(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastAdShownTime
        return timeSinceLastAd >= MIN_AD_INTERVAL
    }

    /**
     * Check if enough actions have occurred since last ad
     */
    private fun hasEnoughActionsPassed(): Boolean {
        return actionsSinceLastAd >= ACTIONS_BETWEEN_ADS
    }

    /**
     * Increment action counter (call this on user actions)
     */
    fun incrementActionCount() {
        actionsSinceLastAd++
    }

    /**
     * Check if ad should be shown based on frequency rules
     */
    fun shouldShowAd(): Boolean {
        return interstitialAd != null &&
               hasEnoughTimePassed() &&
               hasEnoughActionsPassed()
    }

    /**
     * Show the interstitial ad if available and allowed by frequency rules
     * @param activity The activity to show the ad in
     * @param force If true, bypass frequency capping (use sparingly)
     * @return true if ad was shown, false otherwise
     */
    fun showAd(activity: Activity, force: Boolean = false): Boolean {
        val ad = interstitialAd

        if (ad == null) {
            Log.d(TAG, "Ad not ready yet")
            // Ensure we're loading an ad
            if (!isLoading) {
                loadAd()
            }
            return false
        }

        if (!force && !shouldShowAd()) {
            Log.d(TAG, "Frequency capping: not showing ad yet")
            return false
        }

        Log.d(TAG, "Showing interstitial ad")
        ad.show(activity)
        actionsSinceLastAd = 0
        return true
    }

    /**
     * Preload an ad if not already loaded
     */
    fun preloadAd() {
        if (interstitialAd == null && !isLoading) {
            loadAd()
        }
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        interstitialAd = null
        _isAdReady.value = false
        _isAdShowing.value = false
    }
}

/**
 * Composable to manage interstitial ad lifecycle
 * Use this in your app's main composable
 */
@Composable
fun rememberInterstitialAdManager(
    context: Context,
    isProUser: Boolean,
    adUnitId: String = InterstitialAdUnitIds.GENERAL
): InterstitialAdManager? {
    // Don't create ad manager for Pro users
    if (isProUser) {
        return null
    }

    return remember(context) {
        InterstitialAdManager(context, adUnitId)
    }
}

/**
 * Ad Unit IDs for interstitial ads
 */
object InterstitialAdUnitIds {
    const val GENERAL = "ca-app-pub-8461786124508841/8284802515"
    const val AFTER_CAPTURE = "ca-app-pub-8461786124508841/4976112894"
    const val AFTER_SAVE = "ca-app-pub-8461786124508841/3663031226"
}
