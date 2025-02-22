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

package com.example.platform.app

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.fragment.compose.AndroidFragment
import com.example.platform.privacy.transparency.DataAccessSample
import com.example.platform.privacy.transparency.ScreenshotDetectionSample
import com.example.platform.storage.mediastore.MediaStoreQuerySample
import com.example.platform.storage.mediastore.SelectedPhotosAccessSample
import com.example.platform.storage.photopicker.PhotoPickerSample

interface SampleDemo: CatalogItem {
    override val id: String
    override val name: String
    override val description: String?
    val documentation: String?
    val area: Area
    val content: Any
}

data class ComposableSampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val area: Area,
    override val content: @Composable () -> Unit,
) : SampleDemo

data class ActivitySampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val area: Area,
    override val content: Class<out Activity>,
) : SampleDemo

val SAMPLE_DEMOS by lazy {
    listOf(
        ComposableSampleDemo(
            id = "photo-picker",
            name = "PhotoPicker",
            description = "Select images/videos in a privacy-friendly way using the photo picker",
            documentation = "https://developer.android.com/training/data-storage/shared/photopicker",
            area = StorageArea,
            content = { PhotoPickerSample() },
        ),
        ComposableSampleDemo(
            id = "mediastore-query",
            name = "MediaStore - Query",
            description = "Query files indexed by MediaStore",
            documentation = "https://developer.android.com/training/data-storage/shared/media#media_store",
            area = StorageArea,
            content = { MediaStoreQuerySample() },
        ),
        ComposableSampleDemo(
            id = "selected-photos-access",
            name = "Selected Photos Access",
            description = "Check and request storage permissions",
            documentation = "https://developer.android.com/about/versions/14/changes/partial-photo-video-access",
            area = StorageArea,
            content = { SelectedPhotosAccessSample() },
        ),
        ComposableSampleDemo(
            id = "data-access",
            name = "Data Access",
            description = "Demonstrates how to implement data access auditing for your app to identify " +
                    "unexpected data access, even from third-party SDKs and libraries.",
            documentation = "https://developer.android.com/guide/topics/data/audit-access",
            area = PrivacyTransparencyArea,
            content = { DataAccessSample() },
        ),
        ComposableSampleDemo(
            id = "screenshot-detection",
            name = "Screenshot Detection",
            description = "This sample shows how to detect that the user capture the screen in Android 14 onwards",
            documentation = null,
            area = PrivacyTransparencyArea,
            content = { AndroidFragment<ScreenshotDetectionSample>() },
        ),
    ).associateBy { it.id }
}