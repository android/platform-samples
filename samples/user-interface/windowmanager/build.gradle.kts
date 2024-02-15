/*
 * Copyright 2023 Google LLC
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
    id("com.example.platform.sample")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.platform.ui.windowmanager"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.java)
    implementation(libs.androidx.window.rxjava2)
    implementation(libs.androidx.window.core)
    implementation(libs.rxjava2.android)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.core)
    implementation(libs.appcompat)
}
