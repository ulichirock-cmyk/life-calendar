plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.leida.lifecalendar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.leida.lifecalendar"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // --- In-app updater (data/update/) ---
        // The repo is public, so the release lookup and the APK download are both anonymous —
        // nothing secret is baked into the APK.
        buildConfigField("String", "UPDATE_REPO", "\"ulichirock-cmyk/life-calendar\"")
        // Asset name produced by .github/workflows/android-release.yml.
        buildConfigField("String", "UPDATE_ASSET_NAME", "\"app-release.apk\"")
    }

    signingConfigs {
        create("release") {
            // Supplied by CI (see .github/workflows/android-release.yml) or a local build via env
            // vars. With no keystore present the release type is left unsigned rather than failing
            // the build — note that this also renames the output to app-release-unsigned.apk.
            val keystoreFile = System.getenv("SIGNING_KEYSTORE_FILE")?.let { file(it) }
            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // A separate package, so a debug build installs alongside the release instead of
            // fighting it for one install slot. The two are signed with different keys and can
            // never replace each other, so sharing an applicationId would make the very first
            // in-app update impossible (Android refuses release-over-debug outright).
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
}
