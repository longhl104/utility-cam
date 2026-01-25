# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep all data classes and managers (required for Gson, DataStore, and language switching)
# This includes LocaleManager, Language, PreferencesManager, etc.
-keep class com.utility.cam.data.** { *; }

# Keep DataStore classes and methods
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }
-keep class androidx.datastore.preferences.** { *; }
-keep class androidx.datastore.core.** { *; }

# Keep Kotlin metadata for data classes (required for reflection)
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep methods used for locale/configuration changes
-keepclassmembers class * extends android.app.Activity {
    public void attachBaseContext(android.content.Context);
    public void recreate();
}

# Play Core / Play Feature Delivery (for on-demand language resources)
-keep class com.google.android.play.core.** { *; }
-keep interface com.google.android.play.core.** { *; }

# Google Play Services - Keep annotations
-dontwarn com.google.android.gms.common.annotation.**
-keep class com.google.android.gms.common.annotation.** { *; }

# Glance
-keep class androidx.glance.** { *; }

# Biometric Authentication (required for app lock feature)
-keep class androidx.biometric.** { *; }
-keep interface androidx.biometric.** { *; }

# Keep BiometricManager and all nested classes including sealed class
-keep class com.utility.cam.data.BiometricManager { *; }
-keep class com.utility.cam.data.BiometricManager$** { *; }

# Keep BiometricPrompt and its nested classes
-keep class androidx.biometric.BiometricPrompt { *; }
-keep class androidx.biometric.BiometricPrompt$** { *; }

# Keep BiometricPrompt callbacks (required for authentication to work)
-keep class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback { *; }
-keepclassmembers class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback {
    public <methods>;
    *;
}

# Keep all anonymous inner classes that extend AuthenticationCallback
-keep class com.utility.cam.data.BiometricManager$authenticate$** { *; }

# Keep BiometricManager methods (especially authenticate and availability checks)
-keepclassmembers class com.utility.cam.data.BiometricManager {
    public <methods>;
    private <methods>;
    *;
}

# Keep FragmentActivity for BiometricPrompt (required for activity context)
-keep class androidx.fragment.app.FragmentActivity { *; }
-keepclassmembers class * extends androidx.fragment.app.FragmentActivity {
    public <methods>;
}

# Keep sealed class BiometricAvailability and all its subclasses
-keep class com.utility.cam.data.BiometricManager$BiometricAvailability { *; }
-keep class com.utility.cam.data.BiometricManager$BiometricAvailability$** { *; }

# Keep Kotlin sealed classes (important for BiometricAvailability)
-keep class kotlin.reflect.** { *; }
-keep interface kotlin.reflect.** { *; }

# Keep all Kotlin metadata for sealed classes
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Keep names of sealed class objects
-keepnames class com.utility.cam.data.BiometricManager$BiometricAvailability$Available
-keepnames class com.utility.cam.data.BiometricManager$BiometricAvailability$NoHardware
-keepnames class com.utility.cam.data.BiometricManager$BiometricAvailability$HardwareUnavailable
-keepnames class com.utility.cam.data.BiometricManager$BiometricAvailability$NoneEnrolled
-keepnames class com.utility.cam.data.BiometricManager$BiometricAvailability$SecurityUpdateRequired
-keepnames class com.utility.cam.data.BiometricManager$BiometricAvailability$Unsupported
-keepnames class com.utility.cam.data.BiometricManager$BiometricAvailability$Unknown

# ML Kit Document Scanner
-keep class com.google.mlkit.vision.documentscanner.** { *; }
-keep interface com.google.mlkit.vision.documentscanner.** { *; }
-keep class com.google.android.gms.internal.mlkit_document_scanner.** { *; }

-dontwarn java.lang.invoke.StringConcatFactory
