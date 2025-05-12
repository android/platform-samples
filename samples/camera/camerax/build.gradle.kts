/*
 * Copyright 2022 Google LLC
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
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.platform.camerax"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testOptions.targetSdk = 35
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(project(":shared"))

    // CameraX
    implementation(libs.androidx.camerax.core)
    implementation(libs.androidx.camerax.camera2)
    implementation(libs.androidx.camerax.lifecycle)
    implementation(libs.androidx.camerax.video)
    implementation(libs.androidx.camerax.view)
    implementation(libs.androidx.camerax.mlkit.vision)
    implementation(libs.androidx.camerax.mlkit.vision)
    implementation(libs.androidx.camerax.extensions)

    // Image loading
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Barcode Scanning
    implementation(libs.barcode.scanning.common)
    implementation(libs.play.services.mlkit.barcode.scanning)

    // Permissions
    implementation(libs.accompanist.permissions)
}
