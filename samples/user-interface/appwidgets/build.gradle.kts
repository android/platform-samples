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
    id("com.example.platform.sample")
    alias(libs.plugins.kotlin.serialization)
}
    
android {
    namespace = "com.example.platform.ui.appwidgets"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.kotlin.serialization.json)
    // For traditional RemoteViews
    implementation(libs.androidx.core.remoteviews)
    // For building appwidgets with Glance
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    // Recommended to use WorkManager to load data for widgets
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.material)

    debugImplementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)
}