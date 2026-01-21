plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.utility.cam"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.utility.cam"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../utility-cam-release.keystore")
            storePassword = project.findProperty("KEYSTORE_PASSWORD") as String? ?: ""
            keyAlias = "utility-cam"
            keyPassword = project.findProperty("KEY_PASSWORD") as String? ?: ""
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Add native debug symbols
            ndk {
                debugSymbolLevel = "FULL" // or "FULL" for more detailed information
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    // Glance for widgets
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // Gson for JSON
    implementation("com.google.code.gson:gson:2.13.2")

    // Accompanist for permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:8.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.exifinterface:exifinterface:1.4.2")
}
