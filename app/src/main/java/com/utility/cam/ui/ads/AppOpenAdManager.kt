package com.utility.cam.ui.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.utility.cam.analytics.AnalyticsHelper
import java.util.Date

/**
 * App Open Ad Manager
 * Shows full-screen ads when app is opened or brought to foreground
 * Respects frequency capping and Pro user status
 */
class AppOpenAdManager(
    private val application: Application,
    private val isProUserProvider: () -> Boolean
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val AD_UNIT_ID = AppOpenAdUnitIds.APP_OPEN

        // Frequency capping: minimum time between app open ads (4 hours)
        private const val MIN_AD_INTERVAL = 4 * 60 * 60 * 1000L // 4 hours in milliseconds
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var lastAdShownTime: Long = 0
    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Load an app open ad in the background
     */
    private fun loadAd() {
        // Don't load if Pro user
        if (isProUserProvider()) {
            Log.d(TAG, "Pro user - not loading app open ad")
            return
        }

        // Don't load if already loading or ad is available
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            application,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App open ad loaded successfully")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    AnalyticsHelper.logAdLoaded("AppOpen")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Failed to load app open ad: ${loadAdError.message}")
                    isLoadingAd = false
                    AnalyticsHelper.logAdLoadFailed("AppOpen", loadAdError.message)
                }
            }
        )
    }

    /**
     * Check if ad has expired (ads expire after 4 hours)
     */
    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * 4
    }

    /**
     * Check if ad is available and not expired
     */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    /**
     * Check if enough time has passed since last ad
     */
    private fun canShowAd(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastAdShownTime
        return timeSinceLastAd >= MIN_AD_INTERVAL
    }

    /**
     * Show the app open ad if available
     */
    private fun showAdIfAvailable(activity: Activity) {
        // Don't show if Pro user
        if (isProUserProvider()) {
            Log.d(TAG, "Pro user - not showing app open ad")
            loadAd() // Preload for when they might need it
            return
        }

        // Don't show if already showing
        if (isShowingAd) {
            Log.d(TAG, "App open ad is already showing")
            return
        }

        // Check frequency capping
        if (!canShowAd()) {
            Log.d(TAG, "Frequency capping: not showing ad yet (last shown too recently)")
            return
        }

        // Check if ad is available
        if (!isAdAvailable()) {
            Log.d(TAG, "App open ad is not available")
            loadAd()
            return
        }

        Log.d(TAG, "Showing app open ad")
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed")
                appOpenAd = null
                isShowingAd = false
                lastAdShownTime = System.currentTimeMillis()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App open ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed")
                isShowingAd = true
                AnalyticsHelper.logAdClicked("AppOpen")
            }
        }

        appOpenAd?.show(activity)
    }

    /**
     * Lifecycle observer - show ad when app comes to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let { activity ->
            showAdIfAvailable(activity)
        }
        Log.d(TAG, "App foregrounded")
    }

    // Activity Lifecycle Callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (!isShowingAd) {
            loadAd()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (activity == currentActivity) {
            currentActivity = null
        }
    }
}

/**
 * App Open Ad Unit IDs
 */
object AppOpenAdUnitIds {
    const val APP_OPEN = "ca-app-pub-8461786124508841/9462152810"
}
