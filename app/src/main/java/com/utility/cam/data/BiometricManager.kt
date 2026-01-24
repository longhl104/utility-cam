package com.utility.cam.data

import android.content.Context
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
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    /**
     * Check if biometric authentication is available on this device
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = AndroidBiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            AndroidBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
            AndroidBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.Unsupported
            AndroidBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.Unknown
            else -> BiometricAvailability.Unknown
        }
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
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Failed attempt but user can try again
                    // Don't call onError here to allow retry
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                if (subtitle.isNotEmpty()) setSubtitle(subtitle)
                if (description.isNotEmpty()) setDescription(description)
            }
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
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
