plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tuxy.airo"
    compileSdk = 35

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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    testImplementation("org.mockito:mockito-core:4.11.0") // Updated to a common recent version
    testImplementation("org.mockito:mockito-inline:4.11.0") // For mocking final classes/methods if needed
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0") // Align with OkHttp if possible, using common recent
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // For testing coroutines

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
