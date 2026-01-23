package com.utility.cam.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.utility.cam.BuildConfig
import com.utility.cam.data.BillingManager
import com.utility.cam.data.PreferencesManager

/**
 * Composable function that provides Pro user state.
 * Combines actual Pro purchase status with debug override for testing.
 *
 * @return True if user should have Pro access (purchased or debug override enabled)
 */
@Composable
fun rememberProUserState(): Boolean {
    val context = LocalContext.current
    val billingManager = remember { BillingManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }

    val isProUser by billingManager.isProUser.collectAsState()
    val debugProOverride by preferencesManager.getDebugProOverride().collectAsState(initial = false)

    // Debug override is strongest - it must be enabled for Pro access
    return if (BuildConfig.DEBUG) {
        debugProOverride
    } else {
        isProUser
    }
}

/**
 * Data class holding Pro user state and manager instances.
 * Use this when you need access to the managers for other operations.
 */
data class ProUserStateWithManagers(
    val isProUser: Boolean,
    val actualIsProUser: Boolean,
    val billingManager: BillingManager,
    val preferencesManager: PreferencesManager
)

/**
 * Composable function that provides Pro user state along with manager instances.
 * Use this when you need to perform other operations with BillingManager or PreferencesManager.
 *
 * @return ProUserStateWithManagers containing state and manager instances
 */
@Composable
fun rememberProUserStateWithManagers(): ProUserStateWithManagers {
    val context = LocalContext.current
    val billingManager = remember { BillingManager(context) }
    val preferencesManager = remember { PreferencesManager(context) }

    val isProUser by billingManager.isProUser.collectAsState()
    val debugProOverride by preferencesManager.getDebugProOverride().collectAsState(initial = false)
    // Debug override is strongest - it must be enabled for Pro access
    val actualIsProUser = if (BuildConfig.DEBUG) {
        debugProOverride
    } else {
        isProUser
    }

    return ProUserStateWithManagers(
        isProUser = isProUser,
        actualIsProUser = actualIsProUser,
        billingManager = billingManager,
        preferencesManager = preferencesManager
    )
}
