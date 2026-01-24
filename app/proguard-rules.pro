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

-dontwarn java.lang.invoke.StringConcatFactory
