plugins {
    id("com.android.application")
}

android {
    namespace = "ua.pp.chds.ashare"
    compileSdk = 36

    defaultConfig {
        applicationId = "ua.pp.chds.ashare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
