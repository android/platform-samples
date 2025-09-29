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
data class ApiSurface(override val id: String, override val name: String, override val description: String? = null) : CatalogItem

val AccessiblityApiSurface = ApiSurface(
    "accessiblity",
    "Accessibility",
    null,
)

val CameraCamera2ApiSurface = ApiSurface(
    "camera-camera2",
    "Camera2",
    null,
)

val CameraCameraXApiSurface = ApiSurface(
    "camera-camerax",
    "CameraX",
    null,
)

val ConnectivityAudioApiSurface = ApiSurface(
    "connectivity-audio",
    "Connectivity Audio",
    null,
)

val ConnectivityBluetoothBleApiSurface = ApiSurface(
    "connectivity-bluetooth-ble",
    "Connectivity Bluetooth BLE",
    null,
)

val ConnectivityBluetoothCompanionApiSurface = ApiSurface(
    "connectivity-bluetooth-companion",
    "Connectivity Bluetooth Companion",
    null,
)

val ConnectivityCallNotificationApiSurface = ApiSurface(
    "connectivity-call-notification",
    "Connectivity Call Notification",
    null,
)

val ConnectivityTelecomApiSurface = ApiSurface(
    "connectivity-telecom",
    "Connectivity Telecom",
    null,
)

val GraphicsPdfApiSurface = ApiSurface(
    "graphics-pdf",
    "Graphics PDF",
    null,
)

val GraphicsUltraHdrApiSurface = ApiSurface(
    "graphics-ultrahdr",
    "Graphics UltraHDR",
    null,
)

val LocationApiSurface = ApiSurface(
    "location",
    "Location",
    null,
)

val MediaUltraHdrApiSurface = ApiSurface(
    "media-ultrahdr",
    "Media UltraHDR",
    null,
)

val MediaVideoApiSurface = ApiSurface(
    "media-video",
    "Media Video",
    null,
)

val PrivacyDataApiSurface = ApiSurface(
    "privacy-data",
    "Privacy Data",
    null,
)

val PrivacyPermissionsApiSurface = ApiSurface(
    "privacy-permissions",
    "Privacy Permissions",
    null,
)

val PrivacyTransparencyApiSurface = ApiSurface(
    "privacy-transparency",
    "Privacy Transparency",
    null,
)

val StorageApiSurface = ApiSurface(
    "storage",
    "Storage",
    """Android photo library access capabilities.
              Photo Picker for unified device and cloud photo access, and MediaStore for detailed local media querying.
              
              WARNING: These samples only work on devices with Android 14 or higher.""".trimMargin()
)

val UserInterfaceAppWidgetsApiSurface = ApiSurface(
    "user-interface-app-widgets",
    "User Interface - App Widgets",
    null,
)

val UserInterfaceConstraintLayoutApiSurface = ApiSurface(
    "user-interface-constraint-layout",
    "User Interface - Constraint Layout",
    null,
)

val UserInterfaceDragAndDropApiSurface = ApiSurface(
    "user-interface-draganddrop",
    "User Interface - Drag and Drop",
    null,
)

val UserInterfaceHapticsApiSurface = ApiSurface(
    "user-interface-haptics",
    "User Interface - Haptics",
    null,
)

val UserInterfaceLiveUpdatesApiSurface = ApiSurface(
    "live-updates",
    "User Interface - Live Updates",
    null,
)

val UserInterfacePictureInPictureApiSurface = ApiSurface(
    "user-interface-picture-in-picture",
    "User Interface - Picture In Picture",
    null,
)

val UserInterfacePredictiveBackApiSurface = ApiSurface(
    "user-interface-predictive-back",
    "User Interface - Predictive Back",
    null,
)

val UserInterfaceQuickSettingsApiSurface = ApiSurface(
    "user-interface-quick-settings",
    "User Interface - Quick Settings",
    null,
)

val UserInterfaceShareApiSurface = ApiSurface(
    "user-interface-share",
    "User Interface - Share",
    null,
)

val UserInterfaceTextApiSurface = ApiSurface(
    "user-interface-text",
    "User Interface - Text",
    null,
)

val UserInterfaceWindowInsetsApiSurface = ApiSurface(
    "user-interface-window-insets",
    "User Interface - Window Insets",
    null,
)

val UserInterfaceWindowManagerApiSurface = ApiSurface(
    "user-interface-window-manager",
    "User Interface - Window Manager",
    null,
)

val ConnectivityUwbRangingApiSurface = ApiSurface(
    "connectivity-uwb-ranging",
    "Connectivity UWB Ranging",
    null,
)

val API_SURFACES = listOf(
    AccessiblityApiSurface,
    CameraCamera2ApiSurface,
    CameraCameraXApiSurface,
    ConnectivityAudioApiSurface,
    ConnectivityBluetoothBleApiSurface,
    ConnectivityBluetoothCompanionApiSurface,
    ConnectivityCallNotificationApiSurface,
    ConnectivityTelecomApiSurface,
    ConnectivityUwbRangingApiSurface,
    GraphicsPdfApiSurface,
    GraphicsUltraHdrApiSurface,
    LocationApiSurface,
    MediaUltraHdrApiSurface,
    MediaVideoApiSurface,
    PrivacyDataApiSurface,
    PrivacyPermissionsApiSurface,
    PrivacyTransparencyApiSurface,
    StorageApiSurface,
    UserInterfaceAppWidgetsApiSurface,
    UserInterfaceConstraintLayoutApiSurface,
    UserInterfaceDragAndDropApiSurface,
    UserInterfaceHapticsApiSurface,
    UserInterfaceLiveUpdatesApiSurface,
    UserInterfacePictureInPictureApiSurface,
    UserInterfacePredictiveBackApiSurface,
    UserInterfaceQuickSettingsApiSurface,
    UserInterfaceShareApiSurface,
    UserInterfaceTextApiSurface,
    UserInterfaceWindowInsetsApiSurface,
    UserInterfaceWindowManagerApiSurface
).associateBy { it.id }