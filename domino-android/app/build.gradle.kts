plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.adam.domino"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adam.domino"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "2.1.0-online"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildTypes {
        release { isMinifyEnabled = false }
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
