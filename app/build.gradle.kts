/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.1.10"
}

android {
    namespace = "com.example.platform"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.platform"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.fragment.compose)

    implementation(project(":shared"))
    implementation(project(":samples:accessibility"))
    implementation(project(":samples:camera:camera2"))
    implementation(project(":samples:camera:camerax"))
    implementation(project(":samples:connectivity:audio"))
    implementation(project(":samples:connectivity:bluetooth:ble"))
    implementation(project(":samples:connectivity:bluetooth:companion"))
    implementation(project(":samples:connectivity:callnotification"))
    implementation(project(":samples:connectivity:telecom"))
    implementation(project(":samples:graphics:pdf"))
    implementation(project(":samples:graphics:ultrahdr"))
    implementation(project(":samples:location"))
    implementation(project(":samples:media:ultrahdr"))
    implementation(project(":samples:media:video"))
    implementation(project(":samples:privacy:data"))
    implementation(project(":samples:privacy:permissions"))
    implementation(project(":samples:privacy:transparency"))
    implementation(project(":samples:storage"))
    implementation(project(":samples:user-interface:appwidgets"))
    implementation(project(":samples:user-interface:constraintlayout"))
    implementation(project(":samples:user-interface:draganddrop"))
    implementation(project(":samples:user-interface:haptics"))
    implementation(project(":samples:user-interface:live-updates"))
    implementation(project(":samples:user-interface:picture-in-picture"))
    implementation(project(":samples:user-interface:predictiveback"))
    implementation(project(":samples:user-interface:quicksettings"))
    implementation(project(":samples:user-interface:share"))
    implementation(project(":samples:user-interface:text"))
    implementation(project(":samples:user-interface:window-insets"))
    implementation(project(":samples:user-interface:windowmanager"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}