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

val GraphicsPdfArea = Area(
    "graphics-pdf",
    "Graphics PDF",
    null,
)

val GraphicsUltraHdrArea = Area(
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

val MediaVideoArea = Area(
    "media-video",
    "Media Video",
    null,
)

val PrivacyDataArea = Area(
    "privacy-data",
    "Privacy Data",
    null,
)

val PrivacyPermissionsArea = Area(
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

val UserInterfaceAppWidgetsArea = Area(
    "user-interface-app-widgets",
    "User Interface - App Widgets",
    null,
)

val UserInterfaceConstraintLayoutArea = Area(
    "user-interface-constraint-layout",
    "User Interface - Constraint Layout",
    null,
)

val UserInterfaceDragAndDropArea = Area(
    "user-interface-draganddrop",
    "User Interface - Drag and Drop",
    null,
)

val UserInterfaceHapticsArea = Area(
    "user-interface-haptics",
    "User Interface - Haptics",
    null,
)

val UserInterfacePictureInPictureArea = Area(
    "user-interface-picture-in-picture",
    "User Interface - Picture In Picture",
    null,
)

val UserInterfacePredictiveBackArea = Area(
    "user-interface-predictive-back",
    "User Interface - Predictive Back",
    null,
)

val UserInterfaceQuickSettingsArea = Area(
    "user-interface-quick-settings",
    "User Interface - Quick Settings",
    null,
)

val UserInterfaceShareArea = Area(
    "user-interface-share",
    "User Interface - Share",
    null,
)

val UserInterfaceTextArea = Area(
    "user-interface-text",
    "User Interface - Text",
    null,
)

val UserInterfaceWindowInsetsArea = Area(
    "user-interface-window-insets",
    "User Interface - Window Insets",
    null,
)

val UserInterfaceWindowManagerArea = Area(
    "user-interface-window-manager",
    "User Interface - Window Manager",
    null,
)

val AREAS = listOf(
    AccessiblityArea,
    CameraCamera2Area,
    ConnectivityAudioArea,
    ConnectivityBluetoothBleArea,
    ConnectivityBluetoothCompanionArea,
    ConnectivityCallNotificationArea,
    ConnectivityTelecomArea,
    GraphicsPdfArea,
    GraphicsUltraHdrArea,
    LocationArea,
    MediaUltraHdrArea,
    MediaVideoArea,
    PrivacyDataArea,
    PrivacyPermissionsArea,
    PrivacyTransparencyArea,
    StorageArea,
    UserInterfaceAppWidgetsArea,
    UserInterfaceConstraintLayoutArea,
    UserInterfaceDragAndDropArea,
    UserInterfaceHapticsArea,
    UserInterfacePictureInPictureArea,
    UserInterfacePredictiveBackArea,
    UserInterfaceQuickSettingsArea,
    UserInterfaceShareArea,
    UserInterfaceTextArea,
    UserInterfaceWindowInsetsArea,
    UserInterfaceWindowManagerArea
).associateBy { it.id }