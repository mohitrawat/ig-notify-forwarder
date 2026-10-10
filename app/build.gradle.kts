plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// No third-party dependencies on purpose: the whole app is the three files in
// src/main/java, so it can be read end to end before installing.
android {
    namespace = "dev.vorgal.ignotify"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.vorgal.ignotify"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
