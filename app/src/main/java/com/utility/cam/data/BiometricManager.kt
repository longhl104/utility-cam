package com.utility.cam.data

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.utility.cam.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.biometricDataStore by preferencesDataStore(name = "biometric_preferences")

/**
 * Manages biometric authentication (Fingerprint / FaceID) for app lock.
 * Uses Android's BiometricPrompt API which is standard and trustworthy.
 */
class BiometricManager(private val context: Context) {

    companion object {
        private const val TAG = "BiometricManager"
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    /**
     * Check if biometric authentication is available on this device.
     * This checks for BIOMETRIC_STRONG OR DEVICE_CREDENTIAL, so it will return Available
     * if either biometric or device lock (PIN/pattern/password) is set up.
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = AndroidBiometricManager.from(context)
        // Check for biometric OR device credential (PIN, pattern, password)
        val canAuthenticateResult = biometricManager.canAuthenticate(
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
            AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        Log.d(TAG, "isBiometricAvailable() - canAuthenticate result: $canAuthenticateResult")

        val availability = when (canAuthenticateResult) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometric or device credential authentication is available")
                BiometricAvailability.Available
            }
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "No biometric hardware and no device credential")
                BiometricAvailability.NoHardware
            }
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric hardware unavailable")
                BiometricAvailability.HardwareUnavailable
            }
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No biometric credentials enrolled and no device lock set")
                BiometricAvailability.NoneEnrolled
            }
            AndroidBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.w(TAG, "Security update required")
                BiometricAvailability.SecurityUpdateRequired
            }
            AndroidBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.w(TAG, "Biometric authentication unsupported")
                BiometricAvailability.Unsupported
            }
            AndroidBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.w(TAG, "Biometric status unknown")
                BiometricAvailability.Unknown
            }
            else -> {
                Log.w(TAG, "Unknown biometric status: $canAuthenticateResult")
                BiometricAvailability.Unknown
            }
        }

        return availability
    }

    /**
     * Check if biometric lock is enabled
     */
    fun isBiometricEnabled(): Flow<Boolean> {
        return context.biometricDataStore.data.map { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] ?: false
        }
    }

    /**
     * Enable biometric lock
     */
    suspend fun enableBiometric() {
        context.biometricDataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = true
        }
    }

    /**
     * Disable biometric lock
     */
    suspend fun disableBiometric() {
        context.biometricDataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = false
        }
    }

    /**
     * Show biometric authentication prompt
     *
     * @param activity FragmentActivity context required for BiometricPrompt
     * @param title Title for the authentication prompt
     * @param subtitle Subtitle for the authentication prompt
     * @param description Description text for the authentication prompt
     * @param onSuccess Callback when authentication succeeds
     * @param onError Callback when authentication fails or is canceled
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        description: String = "",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit
    ) {
        Log.d(TAG, "authenticate() called")
        Log.d(TAG, "Activity: ${activity::class.simpleName}")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Subtitle: $subtitle")
        Log.d(TAG, "Description: $description")

        val executor = ContextCompat.getMainExecutor(context)
        Log.d(TAG, "Executor created: $executor")

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "onAuthenticationSucceeded")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "onAuthenticationError: errorCode=$errorCode, errString=$errString")
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "onAuthenticationFailed (user can retry)")
                    // Failed attempt but user can try again
                    // Don't call onError here to allow retry
                }
            }
        )
        Log.d(TAG, "BiometricPrompt created: $biometricPrompt")

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                if (subtitle.isNotEmpty()) {
                    Log.d(TAG, "Setting subtitle: $subtitle")
                    setSubtitle(subtitle)
                }
                if (description.isNotEmpty()) {
                    Log.d(TAG, "Setting description: $description")
                    setDescription(description)
                }
            }
            // Use BIOMETRIC_STRONG OR DEVICE_CREDENTIAL to allow both biometric and device lock
            // Note: Cannot use setNegativeButtonText() with DEVICE_CREDENTIAL
            .setAllowedAuthenticators(
                AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        Log.d(TAG, "PromptInfo built successfully with BIOMETRIC_STRONG | DEVICE_CREDENTIAL")

        Log.d(TAG, "Calling biometricPrompt.authenticate(promptInfo)...")
        biometricPrompt.authenticate(promptInfo)
        Log.d(TAG, "biometricPrompt.authenticate() called - dialog should appear now")
    }

    sealed class BiometricAvailability {
        object Available : BiometricAvailability()
        object NoHardware : BiometricAvailability()
        object HardwareUnavailable : BiometricAvailability()
        object NoneEnrolled : BiometricAvailability()
        object SecurityUpdateRequired : BiometricAvailability()
        object Unsupported : BiometricAvailability()
        object Unknown : BiometricAvailability()

        fun isAvailable(): Boolean = this is Available

        fun getErrorMessage(context: Context): String {
            return when (this) {
                is Available -> context.getString(R.string.biometric_availability_available)
                is NoHardware -> context.getString(R.string.biometric_availability_no_hardware)
                is HardwareUnavailable -> context.getString(R.string.biometric_availability_hardware_unavailable)
                is NoneEnrolled -> context.getString(R.string.biometric_availability_none_enrolled)
                is SecurityUpdateRequired -> context.getString(R.string.biometric_availability_security_update_required)
                is Unsupported -> context.getString(R.string.biometric_availability_unsupported)
                is Unknown -> context.getString(R.string.biometric_availability_unknown)
            }
        }
    }
}
