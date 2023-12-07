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
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.example.platform.convention")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.example.platform.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.platform.app"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.platform.app.AppTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.casa.ui)
    implementation(libs.androidx.appcompat)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.coil.base)
    implementation(libs.coil.video)

    // include all available samples.
    val samples: List<String> by project.extra
    samples.forEach {
        implementation(project(it))
    }

    // Testing
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.junit4)
}

tasks.register("syncSamplesInfo", com.example.platform.plugin.SyncSamplesInfo::class.java) {
    projectDir.set(project.rootDir)
}
// Link the assemble task to the sync task.
// TODO: move syncSamplesInfo task here, as this can break isolated projects
afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy(tasks.findByName("syncSamplesInfo"))
}
