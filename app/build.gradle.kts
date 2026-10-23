// AGP 9 compiles Kotlin itself (built-in Kotlin), so there is no kotlin-android plugin.
// The Compose compiler plugin also sets the Kotlin version that AGP uses.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // The navigation keys are @Serializable, so the back stack survives a configuration change
    // (dark mode, rotation) and the death of the process.
    alias(libs.plugins.kotlin.serialization)
    // Room 3 generates its code with KSP only.
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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
        // Raised by one on each merge to master, in the last task of a story, so that the phone
        // shows which version it runs. The app info on the phone shows only versionName, so it
        // follows versionCode.
        versionCode = 2
        versionName = "0.2"

        // The app runs on one phone. ML Kit adds about 7 MB of native code to the APK,
        // and it brings libraries for four ABIs.
        ndk {
            abiFilters += "arm64-v8a"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The device tests run against a copy of the debug app under its own application ID, never
    // against the installed app: Gradle uninstalls the tested app after the run, which would lose
    // the progress data, and the tests would run on that data. The label of the copy is in
    // src/instrumented/res, so that a copy left by an interrupted run is not taken for the app.
    // The JVM unit tests also exist only for this build type.
    buildTypes {
        create("instrumented") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".instrumented"
        }
    }
    testBuildType = "instrumented"

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

// The schema of each database version is exported here and committed: the migration test
// creates the old versions from these files. The Room plugin also puts them into the assets
// of the device tests.
room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    // repeatOnLifecycle: the countdown on home ticks only while the app is visible.
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation3.ui)
    // A ViewModel per screen, cleared when the screen leaves the back stack.
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.mlkit.digital.ink)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // JUnit 4 on the JVM and on the phone: one framework, the Android default.
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    // pressBack: the system back, as the back gesture sends it.
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
}
