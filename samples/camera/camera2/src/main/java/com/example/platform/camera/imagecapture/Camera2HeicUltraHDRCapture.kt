/*
 * Copyright 2025 The Android Open Source Project
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

package com.example.platform.camera.imagecapture

import android.graphics.ImageFormat
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.catalog.framework.annotations.Sample

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Sample(
    name = "HEIC UltraHDR Image Capture",
    description = "This sample demonstrates how to capture a HEIF compressed still image and " +
            "store it using the new HEIC UltraHDR image format using Camera2.",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR", "Camera2"],
)
class Camera2HeicUltraHDRCapture : Camera2UltraHDRCapture() {
    override val ULTRAHDR_FORMAT = ImageFormat.HEIC_ULTRAHDR
}