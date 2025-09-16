import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tuxy.airo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tuxy.airo"
        minSdk = 26
        //noinspection EditedTargetSdkVersion,OldTargetApi
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.all {
            it.jvmArgs("-XX:+EnableDynamicAgentLoading")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
    }
}

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.dynamic.features.fragment)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.activity.ktx)

    // more icons
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.monitor)

    // room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // klaxon
    implementation(libs.klaxon)

    // datastore
    implementation(libs.androidx.datastore.preferences)

    // map-compose
    implementation(libs.mapcompose)

    // browser
    implementation(libs.androidx.browser)
    implementation(libs.compose.html)

    // camera & qr
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.zxing.android.embedded)
    implementation(libs.composed.barcodes)
    implementation(libs.core)

    // Core testing dependencies
    testImplementation(libs.junit) // Already present
    testImplementation(libs.mockito.core) // Updated to a common recent version
    testImplementation(libs.mockito.inline) // For mocking final classes/methods if needed
    testImplementation(libs.mockwebserver) // Align with OkHttp if possible, using common recent
    testImplementation(libs.kotlinx.coroutines.test) // For testing coroutines
    testImplementation(libs.mockito.kotlin)

    // Preferences library (material 3 version!)
    implementation(libs.composeprefs)

    // Room database extras
    implementation(libs.roomdatabasebackup)

    // workmanager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Klaxon is already an implementation dependency, so it's available for tests.
    // If a different version was needed for tests only: testImplementation("com.beust:klaxon:5.5")
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
