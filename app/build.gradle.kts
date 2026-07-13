import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing is loaded from keystore.properties (kept out of git). When the file is absent
// (most local/CI debug builds), the release build falls back to debug signing so it still builds.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

// Secrets (Supabase, Google) live in local.properties (git-ignored), falling back to gradle
// properties / CI env. Never commit real values into gradle.properties.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

fun secret(name: String): String =
    localProperties.getProperty(name) ?: providers.gradleProperty(name).getOrElse("")

val supabaseUrl = secret("genesyx.supabaseUrl")
val supabaseAnonKey = secret("genesyx.supabaseAnonKey")
val googleWebClientId = secret("genesyx.googleWebClientId")
val apiBaseUrl = secret("genesyx.apiBaseUrl")

android {
    namespace = "com.genesyx.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.genesyx.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.0"

        testInstrumentationRunner = "com.genesyx.app.HiltTestRunner"

        // Supabase + Google config from local.properties (git-ignored); fallback to gradle properties.
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        // Environment + Google Cloud / backend API base. Dev default; release overrides env to PROD.
        buildConfigField("String", "GENESYX_ENV", "\"DEV\"")
        buildConfigField("String", "GENESYX_API_BASE_URL", "\"$apiBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "GENESYX_ENV", "\"PROD\"")
            // Falls back to debug signing when keystore.properties is absent, so the release build
            // is still producible locally for testing. Bump versionCode/versionName (defaultConfig)
            // for every Play Store upload.
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room exports the schema JSON here (exportSchema = true) so version bumps are diffable and
    // MigrationTestHelper can validate them. Also expose it to androidTest as assets.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

// Room schema export location (see GenesyxDatabase.exportSchema).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose (BOM-pinned)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Google sign-in (Credential Manager + Google ID token)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.identity.googleid)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room (offline cache)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (session / theme / onboarding flags)
    implementation(libs.androidx.datastore.preferences)

    // WorkManager (pH offline-first sync queue)
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Supabase (remote) + Ktor engine
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)

    // Serialization (explicit pin — previously only transitive)
    implementation(libs.kotlinx.serialization.json)

    // Test — unit (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    // Test — instrumented (androidTest); runner swaps in HiltTestApplication
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// NetworkModule binds LocalAuthService — which ignores the password entirely and mints a fresh
// random user id per sign-in — whenever the Supabase creds are blank. That selector keys off the
// creds, not the build type, so a release cut on a machine without local.properties would ship
// password-free auth and look completely normal. Fail the release build instead.
//
// Checked on the task graph, not in the release {} block: that block is configured on every
// invocation, so throwing there would break debug builds too (which are meant to run local-first
// without creds).
gradle.taskGraph.whenReady {
    val buildingRelease = allTasks.any { task ->
        task.project.name == "app" &&
            (task.name.startsWith("assemble") || task.name.startsWith("bundle")) &&
            task.name.contains("Release")
    }
    if (buildingRelease && (supabaseUrl.isBlank() || supabaseAnonKey.isBlank())) {
        throw GradleException(
            "Release build requires genesyx.supabaseUrl and genesyx.supabaseAnonKey " +
                "(local.properties or gradle properties). Without them the app falls back to " +
                "LocalAuthService, which accepts any password.",
        )
    }
}
