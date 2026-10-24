import javax.inject.Inject

// AGP 9 compiles Kotlin itself (built-in Kotlin), so there is no kotlin-android plugin.
// The Compose compiler plugin also sets the Kotlin version that AGP uses.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // The navigation keys are @Serializable, so the back stack survives a configuration change
    // (dark mode, rotation) and the death of the process; the content bundle is decoded with it.
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
        versionCode = 4
        versionName = "0.4"

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

// The learning content of content/ in the assets: content/bundle.json and content/version.txt
// (see tools/bundle.py). A build output, not in git, so there is no generated file to forget.
val contentBundle = tasks.register<ContentBundleTask>("contentBundle") {
    val repoRoot = layout.projectDirectory.dir("..")
    python = repoRoot.file(".venv/bin/python")
    script = repoRoot.file("tools/content.py")
    // What the validator reads: the content, the docs with the tables of the catalog, and the
    // scripts themselves.
    sources.from(
        repoRoot.dir("content"),
        repoRoot.dir("docs").asFileTree.matching { include("*.md") },
        repoRoot.dir("tools").asFileTree.matching { include("*.py") },
    )
    outputDir = layout.buildDirectory.dir("generated/content")
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(contentBundle, ContentBundleTask::outputDir)
    }
}

/**
 * Validates the content and writes its bundle with `tools/content.py bundle`. A validation error
 * fails the task, and with it the build, so an APK never carries content that failed the
 * validation. The warnings are printed and do not fail it.
 */
abstract class ContentBundleTask : DefaultTask() {
    // Not an input: the Python environment is set up by hand (see pyproject.toml).
    @get:Internal
    abstract val python: RegularFileProperty

    @get:Internal
    abstract val script: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val exec: ExecOperations

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun run() {
        val python = python.get().asFile
        if (!python.exists()) {
            throw GradleException(
                "The Python environment .venv/ is missing: set it up with the commands at the top of pyproject.toml.",
            )
        }
        // No files of an earlier run stay in the assets.
        fileSystem.delete { delete(outputDir) }
        val result = exec.exec {
            commandLine(python, script.get().asFile, "bundle", outputDir.get().asFile)
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            throw GradleException("The content has errors (printed above): tools/content.py validate lists them.")
        }
    }
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
    // The navigation keys and the content bundle (JSON).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mlkit.digital.ink)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    // The settings (see settings/Settings.kt).
    implementation(libs.datastore.preferences)

    // JUnit 4 on the JVM and on the phone: one framework, the Android default.
    testImplementation(libs.junit)
    // Virtual time for the flows that wait on the clock.
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    // The empty activity in which a device test shows a composable of its own; only in the
    // copy of the app that the device tests run against.
    "instrumentedImplementation"(libs.compose.ui.test.manifest)
    // pressBack: the system back, as the back gesture sends it.
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
}
