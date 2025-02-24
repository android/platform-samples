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

val AccessiblityArea = Area(
    "accessiblity",
    "Accessibility",
    null,
)

val CameraCamera2Area = Area(
    "camera-camera2",
    "Camera2",
    null,
)

val ConnectivityAudioArea = Area(
    "connectivity-audio",
    "Connectivity Audio",
    null,
)

val ConnectivityBluetoothBleArea = Area(
    "connectivity-bluetooth-ble",
    "Connectivity Bluetooth BLE",
    null,
)

val ConnectivityBluetoothCompanionArea = Area(
    "connectivity-bluetooth-companion",
    "Connectivity Bluetooth Companion",
    null,
)

val ConnectivityCallNotificationArea = Area(
    "connectivity-call-notification",
    "Connectivity Call Notification",
    null,
)

val ConnectivityTelecomArea = Area(
    "connectivity-telecom",
    "Connectivity Telecom",
    null,
)

val GraphicsPdf = Area(
    "graphics-pdf",
    "Graphics PDF",
    null,
)

val GraphicsUltraHdr = Area(
    "graphics-ultrahdr",
    "Graphics UltraHDR",
    null,
)

val LocationArea = Area(
    "location",
    "Location",
    null,
)

val MediaUltraHdrArea = Area(
    "media-ultrahdr",
    "Media UltraHDR",
    null,
)

val MediaVideo = Area(
    "media-video",
    "Media Video",
    null,
)

val PrivacyData = Area(
    "privacy-data",
    "Privacy Data",
    null,
)

val PrivacyPermissions = Area(
    "privacy-permissions",
    "Privacy Permissions",
    null,
)

val PrivacyTransparencyArea = Area(
    "privacy-transparency",
    "Privacy Transparency",
    null,
)

val StorageArea = Area(
    "storage",
    "Storage",
    "Android photo library access capabilities.\nPhoto Picker for unified device and cloud photo access, and MediaStore for detailed local media querying",
)

val AREAS = listOf(
    AccessiblityArea,
    CameraCamera2Area,
    ConnectivityAudioArea,
    ConnectivityBluetoothBleArea,
    ConnectivityBluetoothCompanionArea,
    ConnectivityCallNotificationArea,
    ConnectivityTelecomArea,
    GraphicsPdf,
    GraphicsUltraHdr,
    LocationArea,
    MediaUltraHdrArea,
    MediaVideo,
    PrivacyData,
    PrivacyPermissions,
    PrivacyTransparencyArea,
    StorageArea,
).associateBy { it.id }