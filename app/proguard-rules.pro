# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes for Gson
-keep class com.utility.cam.data.** { *; }

# Glance
-keep class androidx.glance.** { *; }

-dontwarn java.lang.invoke.StringConcatFactory
