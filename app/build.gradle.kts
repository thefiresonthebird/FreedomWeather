plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appVersionName = "0.2.1"

android {
    namespace = "com.thefiresonthebird.freedomweather"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    // WearOS specific configuration
    buildFeatures {
        compose = true
        buildConfig = true
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    defaultConfig {
        versionName = appVersionName
        applicationId = "com.thefiresonthebird.freedomweather"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        buildConfigField("String", "WEATHER_API_KEY", "\"${localProperties.getProperty("WEATHER_API_KEY")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }



    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

base {
    archivesName.set("FreedomWeather-$appVersionName")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // WearOS core dependencies
    implementation(libs.play.services.wearable)
    implementation(libs.wear)

    // Compose dependencies
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)

    // WearOS specific UI components
    implementation(libs.core.splashscreen)
    implementation(libs.tiles)
    implementation(libs.tiles.material)
    implementation(libs.tiles.tooling.preview)
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation(libs.protolayout)
    implementation(libs.protolayout.material)
    
    // Location services
    implementation(libs.play.services.location)

    // Horologist libraries for WearOS development
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)
    implementation(libs.watchface.complications.data.source.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Guava and Coroutines support
    implementation(libs.guava)
    implementation(libs.kotlinx.coroutines.guava)

    // Testing dependencies
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.tiles.tooling)
}