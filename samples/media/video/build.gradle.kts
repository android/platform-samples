/*
 * Copyright 2023 The Android Open Source Project
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
    id("de.undercouch.download")
}

android {
    namespace = "com.example.platform.media.video"
    viewBinding.isEnabled = true

    androidResources {
        noCompress += "tflite"
    }
}

// Import DownloadModels task for TFLite sample
project.ext.set("ASSET_DIR", "$projectDir/src/main/assets")
project.ext.set("TEST_ASSETS_DIR", "$projectDir/src/androidTest/assets")
// Download default models; if you wish to use your own models then
// place them in the "assets" directory and comment out this line.
apply {
    from("download_model.gradle")
}

dependencies {
    // Media3 Common
    implementation(libs.androidx.media3.common)

    // Media3 Transformer
    implementation(libs.androidx.media3.transformer)

    // Media3 ExoPlayer
    implementation(libs.androidx.media3.exoplayer)

    // Media3 Ui
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.effect)
    implementation(libs.material)

    // Tensorflow lite dependencies
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.gpu.delegate.plugin)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.select.tf.ops)
}