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
//@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}
    
android {
    namespace = "com.example.platform.ui.appwidgets"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
    lint {
        targetSdk = 35
    }
    testOptions {
        targetSdk = 35
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.conscryptMode", "OFF")
                val robolectricM2Dir = file(
                    "${System.getProperty("user.home")}/.m2/repository/org/robolectric/android-all-instrumented/15-robolectric-12650502-i7"
                )
                if (robolectricM2Dir.exists()) {
                    it.systemProperty("robolectric.offline", "true")
                    it.systemProperty("robolectric.dependency.dir", robolectricM2Dir.absolutePath)
                }
                val asmCacheDir = file("${System.getProperty("user.home")}/.gradle/caches/modules-2/files-2.1/org.ow2.asm")
                if (asmCacheDir.exists()) {
                    val asm99Jars = fileTree(asmCacheDir) {
                        include("**/9.9/**/*.jar")
                    }
                    it.classpath = asm99Jars + it.classpath
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.compose.material.iconsext)

    implementation(libs.coil)
    implementation(libs.coil.compose)

    implementation(libs.kotlinx.serialization.json)

    // For building appwidgets with Glance
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // For building appwidgets with Remote Compose
    implementation(libs.androidx.compose.remote.core)
    implementation(libs.androidx.compose.remote.creation)
    implementation(libs.androidx.compose.remote.creation.compose)
    // Recommended to use WorkManager to load data for widgets
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.material)

    debugImplementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)

    testImplementation(libs.junit4)
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14")
}
