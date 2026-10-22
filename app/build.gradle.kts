// AGP 9 compiles Kotlin itself (built-in Kotlin), so there is no kotlin-android plugin.
// The Compose compiler plugin also sets the Kotlin version that AGP uses.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // The application ID cannot change without a new app and the loss of the progress data.
    // It is the repository name with the hyphen replaced by an underscore (JLS §6.1).
    namespace = "com.somkov.greek_b1"

    // compileSdk is the installed platform; targetSdk is the highest API level of this AGP;
    // minSdk is the API level of the phone, the only device the app runs on.
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "com.somkov.greek_b1"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        // The app runs on one phone. ML Kit brings native libraries for four ABIs.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    // The installed app accepts updates only with the same key, and a new key needs an
    // uninstall, which loses the progress data. So the key is kept in the repository,
    // encrypted with git-crypt (see .gitattributes). The other values are the debug defaults.
    // If the file is missing or still encrypted, the build fails: AGP does not make a new key.
    // A key on the YubiKey (PIV, PKCS#11) was rejected: AGP cannot sign with it, and a key
    // made on the YubiKey cannot be exported, so losing the YubiKey would lose the key.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.mlkit.digital.ink)
}
