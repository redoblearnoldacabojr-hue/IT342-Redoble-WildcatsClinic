plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

import java.io.FileInputStream
import java.util.Properties

// Compute backend URL early so buildTypes and defaultConfig can reference it.
val backendUrl: String = System.getenv("BACKEND_URL") ?: "https://it342-redoble-wildcatsclinic.onrender.com"

android {
    namespace = "com.example.mobile"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        // Provide a compile-time backend URL via BuildConfig. This can be overridden
        // by setting the BACKEND_URL environment variable when building, or by
        // editing the default below.
        val backendUrl = System.getenv("BACKEND_URL") ?: "https://it342-redoble-wildcatsclinic.onrender.com"
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Load keystore properties if present at the project root (keystore.properties)
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            // Optional: set these in keystore.properties if you want automated signing
            if (keystoreProperties.containsKey("storeFile")) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
            }
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
            storePassword = keystoreProperties.getProperty("storePassword", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use the release signing config if available
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            // Expose BACKEND_URL to debug builds as well (same value by default)
            buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    // Fragment KTX for commit/replace extensions
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    // Secure storage for tokens
    implementation("androidx.security:security-crypto:1.1.0-alpha03")
    // Firebase messaging for push notifications
    implementation("com.google.firebase:firebase-messaging:23.0.8")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}