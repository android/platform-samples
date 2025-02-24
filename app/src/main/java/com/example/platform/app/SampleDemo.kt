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
import com.example.platform.accessibility.ColorContrast
import com.example.platform.accessibility.LiveRegionView
import com.example.platform.accessibility.SpeakableText
import com.example.platform.camera.imagecapture.Camera2ImageCapture
import com.example.platform.camera.imagecapture.Camera2UltraHDRCapture
import com.example.platform.camera.preview.Camera2Preview
import com.example.platform.connectivity.audio.AudioCommsSample
import com.example.platform.connectivity.bluetooth.ble.BLEScanIntentSample
import com.example.platform.connectivity.bluetooth.ble.ConnectGATTSample
import com.example.platform.connectivity.bluetooth.ble.FindBLEDevicesSample
import com.example.platform.connectivity.bluetooth.ble.server.GATTServerSample
import com.example.platform.connectivity.bluetooth.cdm.CompanionDeviceManagerSample
import com.example.platform.connectivity.callnotification.CallNotificationSample
import com.example.platform.connectivity.telecom.TelecomCallSample
import com.example.platform.graphics.pdf.PdfRendererScreen
import com.example.platform.graphics.ultrahdr.display.CompressingUltraHDRImages
import com.example.platform.graphics.ultrahdr.display.DisplayUltraHDRScreen
import com.example.platform.graphics.ultrahdr.display.DisplayingUltraHDR
import com.example.platform.graphics.ultrahdr.display.DisplayingUltraHDRUsing3PLibrary
import com.example.platform.graphics.ultrahdr.display.VisualizingAnUltraHDRGainmap
import com.example.platform.graphics.ultrahdr.edit.EditingUltraHDR
import com.example.platform.graphics.ultrahdr.opengl.UltraHDRWithOpenGL
import com.example.platform.location.bglocationaccess.BgLocationAccessScreen
import com.example.platform.location.currentLocation.CurrentLocationScreen
import com.example.platform.location.geofencing.GeofencingScreen
import com.example.platform.location.locationupdates.LocationUpdatesScreen
import com.example.platform.location.permission.LocationPermissionScreen
import com.example.platform.location.useractivityrecog.UserActivityRecognitionScreen
import com.example.platform.media.ultrahdr.video.UltraHDRToHDRVideo
import com.example.platform.media.video.TransformerTFLite
import com.example.platform.media.video.TransformerVideoComposition
import com.example.platform.privacy.data.PackageVisibility
import com.example.platform.privacy.permissions.ComposePermissions
import com.example.platform.privacy.permissions.MultiplePermissions
import com.example.platform.privacy.permissions.Permissionless
import com.example.platform.privacy.permissions.SinglePermission
import com.example.platform.privacy.transparency.DataAccessSample
import com.example.platform.privacy.transparency.ScreenshotDetectionSample
import com.example.platform.storage.mediastore.MediaStoreQuerySample
import com.example.platform.storage.mediastore.SelectedPhotosAccessSample
import com.example.platform.storage.photopicker.PhotoPickerSample

interface SampleDemo : CatalogItem {
    override val id: String
    override val name: String
    override val description: String?
    val documentation: String?
    val tags: List<String>
    val area: Area
    val content: Any
}

data class ComposableSampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val area: Area,
    override val tags: List<String> = emptyList(),
    override val content: @Composable () -> Unit,
) : SampleDemo

data class ActivitySampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val area: Area,
    override val tags: List<String> = emptyList(),
    override val content: Class<out Activity>,
) : SampleDemo

val SAMPLE_DEMOS by lazy {
    listOf(
        ComposableSampleDemo(
            id = "color-contrast",
            name = "Color Contrast",
            description = "This sample demonstrates the importance of proper color contrast and how to " +
                    "audit your app to ensure proper color contrast.",
            documentation = "https://support.google.com/accessibility/android/answer/7158390",
            area = AccessiblityArea,
            content = { ColorContrast() },
        ),
        ComposableSampleDemo(
            id = "live-region-view",
            name = "Live Region (View)",
            description = "Utilize LiveRegion to automatically notify users of accessibility services" +
                    " about changes to a view",
            documentation = "https://developer.android.com/reference/android/view/View#attr_android:accessibilityLiveRegion",
            area = AccessiblityArea,
            content = { AndroidFragment<LiveRegionView>() },
        ),
        ComposableSampleDemo(
            id = "speakable-text",
            name = "Speakable Text",
            description = "The sample demonstrates the importance of having proper labels for" +
                    " interactive elements and how to audit your app for content label related " +
                    "improvements.",
            documentation = "https://developer.android.com/guide/topics/ui/accessibility/apps#describe-ui-element",
            area = AccessiblityArea,
            content = { SpeakableText() },
        ),
        ComposableSampleDemo(
            id = "image-capture",
            name = "Image Capture",
            description = "This sample demonstrates how to capture an image using Camera2 and encode it " +
                    "into a JPEG container.",
            documentation = "https://developer.android.com/training/camera2/capture-sessions-requests",
            area = CameraCamera2Area,
            tags = listOf("Camera2"),
            content = { AndroidFragment<Camera2ImageCapture>() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-image-capture",
            name = "UltraHDR Image Capture",
            description = "This sample demonstrates how to capture a 10-bit compressed still image and " +
                    "store it using the new UltraHDR image format using Camera2.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = CameraCamera2Area,
            tags = listOf("UltraHDR", "Camera2"),
            content = { AndroidFragment<Camera2UltraHDRCapture>() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-image-capture",
            name = "Camera2 Preview",
            description = "Demonstrates displaying processed pixel data directly from the camera sensor "
                    + "to the screen using Camera2.",
            documentation = "https://developer.android.com/training/camera2",
            area = CameraCamera2Area,
            tags = listOf("Camera2"),
            content = { AndroidFragment<Camera2Preview>() },
        ),
        ComposableSampleDemo(
            id = "communication-audio-manager",
            name = "Communication Audio Manager",
            description = "This sample shows how to use audio manager to for Communication application that self-manage the call.",
            documentation = "https://developer.android.com/guide/topics/connectivity/ble-audio/audio-manager",
            area = ConnectivityAudioArea,
            tags = listOf("Audio"),
            content = { AudioCommsSample() },
        ),
        ComposableSampleDemo(
            id = "create-gatt-server",
            name = "Create a GATT server",
            description = "Shows how to create a GATT server and communicate with the GATT client",
            documentation = "https://developer.android.com/reference/android/bluetooth/BluetoothGattServer",
            area = ConnectivityBluetoothBleArea,
            tags = listOf("Bluetooth"),
            content = { GATTServerSample() },
        ),
        ComposableSampleDemo(
            id = "scan-with-ble-intent",
            name = "Scan with BLE Intent",
            description = "This samples shows how to use the BLE intent to scan for devices",
            documentation = "https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(java.util.List%3Candroid.bluetooth.le.ScanFilter%3E,%20android.bluetooth.le.ScanSettings,%20android.app.PendingIntent)",
            area = ConnectivityBluetoothBleArea,
            tags = listOf("Bluetooth"),
            content = { BLEScanIntentSample() },
        ),
        ComposableSampleDemo(
            id = "connect-gatt-server",
            name = "Connect to a GATT server",
            description = "Shows how to connect to a GATT server hosted by the BLE device and perform simple operations",
            documentation = "https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server",
            area = ConnectivityBluetoothBleArea,
            tags = listOf("Bluetooth"),
            content = { ConnectGATTSample() },
        ),
        ComposableSampleDemo(
            id = "find-devices",
            name = "Find devices",
            description = "This example will demonstrate how to scanning for Low Energy Devices",
            documentation = "https://developer.android.com/guide/topics/connectivity/bluetooth",
            area = ConnectivityBluetoothBleArea,
            tags = listOf("Bluetooth"),
            content = { FindBLEDevicesSample() },
        ),
        ComposableSampleDemo(
            id = "companion-device-manager",
            name = "Companion Device Manager",
            description = "This samples shows how to use the CDM to pair and connect with BLE devices",
            documentation = "https://developer.android.com/guide/topics/connectivity/companion-device-pairing",
            area = ConnectivityBluetoothCompanionArea,
            tags = listOf("Bluetooth"),
            content = { CompanionDeviceManagerSample() },
        ),
        ActivitySampleDemo(
            id = "call-notification",
            name = "Call Notification",
            description = "Sample demonstrating how to make incoming call notifications and in call notifications",
            documentation = "https://developer.android.com/reference/android/app/Notification.CallStyle",
            area = ConnectivityCallNotificationArea,
            content = CallNotificationSample::class.java,
        ),
        ComposableSampleDemo(
            id = "telecom-call",
            name = "Telecom Call",
            description = "A sample showcasing how to handle calls with the Jetpack Telecom API",
            documentation = "https://developer.android.com/guide/topics/connectivity/telecom",
            area = ConnectivityTelecomArea,
            content = { TelecomCallSample() },
        ),
        ComposableSampleDemo(
            id = "pdf-renderer",
            name = "PDF Renderer",
            description = "Demonstrates how to use PdfRenderer to display PDF documents on the screen.",
            documentation = null,
            area = GraphicsPdf,
            content = { PdfRendererScreen() },
        ),
        ComposableSampleDemo(
            id = "compressing-ultrahdr-images",
            name = "Compressing UltraHDR Images",
            description = "This sample demonstrates displaying an UltraHDR image in a Compose View and an Android View",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = GraphicsUltraHdr,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<CompressingUltraHDRImages>() },
        ),
        ComposableSampleDemo(
            id = "displaying-ultrahdr",
            name = "Displaying UltraHDR",
            description = "This sample demonstrates displaying an UltraHDR image.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = GraphicsUltraHdr,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<DisplayingUltraHDR>() },
        ),
        ComposableSampleDemo(
            id = "displaying-ultrahdr",
            name = "Displaying UltraHDR (3P Libraries)",
            description = "This sample demonstrates using the various popular image loading library to" +
                    " detect the presence of a gainmap to enable HDR mode when displaying an UltraHDR image",
            documentation = "https://github.com/bumptech/glide",
            area = GraphicsUltraHdr,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<DisplayingUltraHDRUsing3PLibrary>() },
        ),
        ComposableSampleDemo(
            id = "displaying-ultrahdr-compose",
            name = "Displaying UltraHDR (Compose)",
            description = "This sample demonstrates displaying an UltraHDR image in a Compose View and an Android View",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = GraphicsUltraHdr,
            tags = listOf("UltraHDR", "Compose"),
            content = { DisplayUltraHDRScreen() },
        ),
        ComposableSampleDemo(
            id = "visualizing-ultrahdr-gainmap",
            name = "Visualizing an UltraHDR Gainmap",
            description = "This sample demonstrates visualizing the underlying gainmap of an UltraHDR " +
                    "image, which reveals which parts of the image are enhanced by the gainmap.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = GraphicsUltraHdr,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<VisualizingAnUltraHDRGainmap>() },
        ),
        ComposableSampleDemo(
            id = "editing-ultrahdr",
            name = "Editing UltraHDR",
            description = "This sample demonstrates editing an UltraHDR image and the resulting gainmap as well. Spatial edit operations like crop, rotate, scale are supported",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = GraphicsUltraHdr,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<EditingUltraHDR>() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-opengles-surfaceview",
            name = "UltraHDR x OpenGLES SurfaceView",
            description = "This sample demonstrates displaying an UltraHDR image via and OpenGL Pipeline " +
                    "and control the SurfaceView's rendering brightness.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = GraphicsUltraHdr,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<UltraHDRWithOpenGL>() },
        ),
        ComposableSampleDemo(
            id = "location-background-location-updates",
            name = "Location - Background Location updates",
            description = "This Sample demonstrate how to access location and get location updates when app is in background",
            documentation = "https://developer.android.com/training/location/background",
            area = LocationArea,
            content = { BgLocationAccessScreen() },
        ),
        ComposableSampleDemo(
            id = "location-getting-current-location",
            name = "Location - Getting Current Location",
            description = "This Sample demonstrate how to request of current location",
            documentation = "https://developer.android.com/training/location/retrieve-current",
            area = LocationArea,
            content = { CurrentLocationScreen() },
        ),
        ComposableSampleDemo(
            id = "location-create-monitor-geofence",
            name = "Location - Create and monitor Geofence",
            description = "This Sample demonstrate best practices for Creating and monitoring geofence",
            documentation = "https://developer.android.com/training/location/geofencing",
            area = LocationArea,
            content = { GeofencingScreen() },
        ),
        ComposableSampleDemo(
            id = "location-updates",
            name = "Location - Updates",
            description = "This Sample demonstrate how to get location updates",
            documentation = "https://developer.android.com/training/location/request-updates",
            area = LocationArea,
            content = { LocationUpdatesScreen() },
        ),
        ComposableSampleDemo(
            id = "location-permissions",
            name = "Location - Permissions",
            description = "This Sample demonstrate best practices for Location Permission",
            documentation = "https://developer.android.com/training/location/permissions",
            area = LocationArea,
            tags = listOf("permissions"),
            content = { LocationPermissionScreen() },
        ),
        ComposableSampleDemo(
            id = "location-user-activity-recognition",
            name = "Location - User Activity Recognition",
            description = "This Sample demonstrate detection of user activity like walking, driving, etc.",
            documentation = "https://developer.android.com/training/location/transitions",
            area = LocationArea,
            content = { UserActivityRecognitionScreen() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-to-hdr-video",
            name = "UltraHDR to HDR Video",
            description = "This sample demonstrates converting a series of UltraHDR images into a HDR " +
                    "video." + "The sample leverages GPU hardware acceleration to render and encode the " +
                    "images.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            area = MediaUltraHdrArea,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<UltraHDRToHDRVideo>() },
        ),
        ComposableSampleDemo(
            id = "transformer-tflite",
            name = "Transformer and TFLite",
            description = "This sample demonstrates using Transformer with TFLite/RTLite by applying a selected art style to a video.",
            documentation = "https://developer.android.com/guide/topics/media/transformer",
            area = MediaVideo,
            tags = listOf("Transformer"),
            content = { AndroidFragment<TransformerTFLite>() },
        ),
        ComposableSampleDemo(
            id = "video-composition-using-media3-transformer",
            name = "Video Composition using Media3 Transformer",
            description = "This sample demonstrates concatenation of two video assets and an image using Media3 Transformer library.",
            documentation = "https://developer.android.com/guide/topics/media/transformer",
            area = MediaVideo,
            tags = listOf("Transformer"),
            content = { AndroidFragment<TransformerVideoComposition>() },
        ),
        ComposableSampleDemo(
            id = "package-visibility",
            name = "Package Visibility",
            description = "A sample that showcase how the package visibility queries affects the available packages",
            documentation = "https://developer.android.com/training/package-visibility",
            area = PrivacyData,
            content = { PackageVisibility() },
        ),
        ComposableSampleDemo(
            id = "permissions-compose",
            name = "Permissions using Compose",
            description = "This sample showcases how to request permission using Accompanist in Compose",
            documentation = "https://google.github.io/accompanist/permissions/",
            area = PrivacyPermissions,
            tags = listOf("Permissions"),
            content = { ComposePermissions() },
        ),
        ComposableSampleDemo(
            id = "permissions-compose",
            name = "Multiple Permissions",
            description = "Shows the recommended flow to request multiple RELATED runtime permissions",
            documentation = "https://developer.android.com/training/permissions/requesting",
            area = PrivacyPermissions,
            tags = listOf("Permissions"),
            content = { AndroidFragment<MultiplePermissions>() },
        ),
        ComposableSampleDemo(
            id = "permissionless",
            name = "Permissionless",
            description = "This sample demonstrate how you can avoid requesting permission for certain actions by leveraging System APIs",
            documentation = "https://developer.android.com/training/permissions/evaluating",
            area = PrivacyPermissions,
            tags = listOf("Permissions"),
            content = { Permissionless() },
        ),
        ComposableSampleDemo(
            id = "single-permission",
            name = "Single Permission",
            description = "Shows the recommended flow to request single runtime permissions",
            documentation = "https://developer.android.com/training/permissions/requesting",
            area = PrivacyPermissions,
            tags = listOf("Permissions"),
            content = { AndroidFragment<SinglePermission>() },
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
    ).associateBy { it.id }
}