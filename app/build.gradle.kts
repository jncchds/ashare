plugins {
    id("com.android.application")
}

// Release signing comes from the environment (set by CI from the repo secrets).
// Without it, assembleRelease still works but produces an unsigned APK.
val keystoreFile: String? = System.getenv("ANDROID_KEYSTORE_FILE")

android {
    namespace = "ua.pp.chds.ashare"
    compileSdk = 36

    defaultConfig {
        applicationId = "ua.pp.chds.ashare"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                // No separate key password secret: the key uses the keystore password.
                keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
