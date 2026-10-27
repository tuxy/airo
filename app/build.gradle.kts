plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.openapi.generator")
}

android {
    namespace = "com.tuxy.airo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tuxy.airo"
        minSdk = 26
        //noinspection EditedTargetSdkVersion,OldTargetApi
        targetSdk = 35
        versionCode = providers.gradleProperty("versionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = providers.environmentVariable("RELEASE_STORE_FILE").orNull
            if (!storePath.isNullOrBlank()) {
                storeFile = rootProject.file(storePath)
                storePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Local builds fall back to the debug key; CI must provide the
            // release keystore through the RELEASE_* environment variables.
            signingConfig = signingConfigs.findByName("release")
                ?.takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.jvmArgs("-XX:+EnableDynamicAgentLoading")
        }
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    sourceSets {
        getByName("main") {
            kotlin.directories.add("$projectDir/build/openapi/src/main/kotlin")
            java.directories.add("$projectDir/build/openapi/src/main/kotlin")
        }
    }
}

openApiGenerate {
    inputSpec.set("$rootDir/openapi/openapi-apimarket-v1.yaml")
    outputDir.set("$projectDir/build/openapi")
    generatorName.set("kotlin")
    skipValidateSpec.set(true)
    importMappings.put("OffsetDateTime", "kotlin.String")

    globalProperties.put("models", "")
    globalProperties.put("modelDocs", "false")
    globalProperties.put("apiDocs", "false")
}

// Ensure generated OpenAPI models are available to KSP / Kotlin compilation
tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn("openApiGenerate")
}

dependencies {
    implementation(libs.androidx.navigation.compose)
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation)

    // more icons
    implementation(libs.androidx.material.icons.extended)

    // room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // klaxon
    implementation(libs.moshi.kotlin)

    // datastore
    implementation(libs.androidx.datastore.preferences)

    // map-compose & other map utils
    implementation(libs.maplibre.android.sdk)

    // browser
    implementation(libs.androidx.browser)
    implementation(libs.compose.html)

    // camera & qr
    implementation(libs.zxing.android.embedded)
    implementation(libs.composed.barcodes)
    implementation(libs.core)
    implementation(libs.accompanist.permissions)

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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}
