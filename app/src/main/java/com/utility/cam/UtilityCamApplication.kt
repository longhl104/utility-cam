package com.utility.cam

import android.app.Application
import com.utility.cam.data.PreferencesManager
import com.utility.cam.data.BillingManager
import com.utility.cam.ui.ads.AppOpenAdManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class UtilityCamApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()

        // Initialize preferences and billing
        preferencesManager = PreferencesManager(this)
        billingManager = BillingManager(this)

        // Initialize App Open Ad Manager with Pro user check
        appOpenAdManager = AppOpenAdManager(
            application = this,
            isProUserProvider = {
                // Check if user is Pro (blocking call, but only used for ad decisions)
                runBlocking {
                    val debugOverride = preferencesManager.getDebugProOverride().first()
                    val hasPurchased = billingManager.isProUser.first()
                    debugOverride || hasPurchased
                }
            }
        )
    }
}
