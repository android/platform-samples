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

import kotlinx.serialization.Serializable

@Serializable
data class Area(override val id: String, override val name: String, override val description: String? = null) : CatalogItem

val StorageArea = Area(
    "storage",
    "Storage",
    "Android photo library access capabilities.\nPhoto Picker for unified device and cloud photo access, and MediaStore for detailed local media querying",
)

val PrivacyTransparencyArea = Area(
    "privacy-transparency",
    "Privacy Transparency",
    null,
)

val AREAS = listOf(
    StorageArea,
    PrivacyTransparencyArea,
).associateBy { it.id }


//val areasSamples = mapOf(
//    StorageArea to listOf(
//        PhotoPickerSampleDemo,
//        MediaStoreQuerySampleDemo,
//        SelectedPhotosAccessSampleDemo,
//    ),
//    PrivacyTransparencyArea to listOf(DataAccessSampleDemo, ScreenshotDetectionSampleDemo),
//)