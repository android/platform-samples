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

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.fragment.compose.AndroidFragment
import com.example.android.pip.PiPMovieActivity
import com.example.android.pip.PiPSampleActivity
import com.example.platform.accessibility.ColorContrast
import com.example.platform.accessibility.LiveRegionView
import com.example.platform.accessibility.SpeakableText
import com.example.platform.camera.imagecapture.Camera2ImageCapture
import com.example.platform.camera.imagecapture.Camera2UltraHDRCapture
import com.example.platform.camera.preview.Camera2Preview
import com.example.platform.camerax.CameraXBasic
import com.example.platform.camerax.CameraXExtensions
import com.example.platform.camerax.CameraXMlKitScreen
import com.example.platform.camerax.CameraXVideo
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
import com.example.platform.shared.MinSdkBox
import com.example.platform.storage.mediastore.MediaStoreQuerySample
import com.example.platform.storage.mediastore.SelectedPhotosAccessSample
import com.example.platform.storage.photopicker.PhotoPickerSample
import com.example.platform.ui.appwidgets.AppWidgets
import com.example.platform.ui.constraintlayout.AdvancedArrangementFragment
import com.example.platform.ui.constraintlayout.AdvancedChainsFragment
import com.example.platform.ui.constraintlayout.AspectRatioFragment
import com.example.platform.ui.constraintlayout.BasicArrangementFragment
import com.example.platform.ui.constraintlayout.BasicChainFragment
import com.example.platform.ui.constraintlayout.CenteringViewsFragment
import com.example.platform.ui.constraintlayout.ComplexMotion1Fragment
import com.example.platform.ui.constraintlayout.ComplexMotion2Fragment
import com.example.platform.ui.constraintlayout.ComplexMotion3Fragment
import com.example.platform.ui.constraintlayout.ComplexMotion4Fragment
import com.example.platform.ui.constraintlayout.ConstraintSetFragment
import com.example.platform.ui.constraintlayout.Coordinator1Fragment
import com.example.platform.ui.constraintlayout.Coordinator2Fragment
import com.example.platform.ui.constraintlayout.Coordinator3Fragment
import com.example.platform.ui.constraintlayout.CustomAttributeFragment
import com.example.platform.ui.constraintlayout.Drawer1Fragment
import com.example.platform.ui.constraintlayout.Drawer2Fragment
import com.example.platform.ui.constraintlayout.FragmentTransition2Fragment
import com.example.platform.ui.constraintlayout.FragmentTransitionFragment
import com.example.platform.ui.constraintlayout.GuidelinesFragment
import com.example.platform.ui.constraintlayout.ImageFilter1Fragment
import com.example.platform.ui.constraintlayout.ImageFilter2Fragment
import com.example.platform.ui.constraintlayout.KeyTriggerFragment
import com.example.platform.ui.constraintlayout.KeyframeCycleFragment
import com.example.platform.ui.constraintlayout.KeyframeInterpolationFragment
import com.example.platform.ui.constraintlayout.KeyframePositionFragment
import com.example.platform.ui.constraintlayout.LottieFragment
import com.example.platform.ui.constraintlayout.MotionBasic01Fragment
import com.example.platform.ui.constraintlayout.MotionBasic02Fragment
import com.example.platform.ui.constraintlayout.MotionBasic02NoAutoCompleteFragment
import com.example.platform.ui.constraintlayout.MultiStateFragment
import com.example.platform.ui.constraintlayout.ParallaxFragment
import com.example.platform.ui.constraintlayout.SidePanelFragment
import com.example.platform.ui.constraintlayout.ViewPagerFragment
import com.example.platform.ui.constraintlayout.YoutubeFragment
import com.example.platform.ui.draganddrop.DragAndDropCompose
import com.example.platform.ui.draganddrop.DragAndDropMultiWindow
import com.example.platform.ui.draganddrop.DragAndDropRichContentReceiverFragment
import com.example.platform.ui.draganddrop.DragAndDropWithHelper
import com.example.platform.ui.draganddrop.DragAndDropWithViews
import com.example.platform.ui.haptics.Bounce
import com.example.platform.ui.haptics.Expand
import com.example.platform.ui.haptics.HapticsBasic
import com.example.platform.ui.haptics.Resist
import com.example.platform.ui.haptics.Wobble
import com.example.platform.ui.insets.ImmersiveMode
import com.example.platform.ui.insets.WindowInsetsAnimationActivity
import com.example.platform.ui.live_updates.LiveUpdateSample
import com.example.platform.ui.predictiveback.PBHostingActivity
import com.example.platform.ui.quicksettings.QuickSettings
import com.example.platform.ui.share.receiver.ShareReceiverActivity
import com.example.platform.ui.share.sender.ShareSender
import com.example.platform.ui.text.ConversionSuggestions
import com.example.platform.ui.text.DownloadableFontsFragment
import com.example.platform.ui.text.Hyphenation
import com.example.platform.ui.text.LineBreak
import com.example.platform.ui.text.Linkify
import com.example.platform.ui.text.TextSpanFragment
import com.example.platform.ui.windowmanager.demos.WindowDemosActivity

interface SampleDemo : CatalogItem {
    override val id: String
    override val name: String
    override val description: String?
    val documentation: String?
    val minSdk: Int
    val tags: List<String>
    val apiSurface: ApiSurface
    val content: Any
}

data class ComposableSampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val minSdk: Int = Build.VERSION_CODES.LOLLIPOP,
    override val apiSurface: ApiSurface,
    override val tags: List<String> = emptyList(),
    override val content: @Composable () -> Unit,
) : SampleDemo

data class ActivitySampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val minSdk: Int = Build.VERSION_CODES.LOLLIPOP,
    override val apiSurface: ApiSurface,
    override val tags: List<String> = emptyList(),
    override val content: Class<*>,
) : SampleDemo

val SAMPLE_DEMOS by lazy {
    listOf(
        ComposableSampleDemo(
            id = "color-contrast",
            name = "Color Contrast",
            description = "This sample demonstrates the importance of proper color contrast and how to " +
                    "audit your app to ensure proper color contrast.",
            documentation = "https://support.google.com/accessibility/android/answer/7158390",
            apiSurface = AccessiblityApiSurface,
            content = { ColorContrast() },
        ),
        ComposableSampleDemo(
            id = "live-region-view",
            name = "Live Region (View)",
            description = "Utilize LiveRegion to automatically notify users of accessibility services" +
                    " about changes to a view",
            documentation = "https://developer.android.com/reference/android/view/View#attr_android:accessibilityLiveRegion",
            apiSurface = AccessiblityApiSurface,
            content = { AndroidFragment<LiveRegionView>() },
        ),
        ComposableSampleDemo(
            id = "speakable-text",
            name = "Speakable Text",
            description = "The sample demonstrates the importance of having proper labels for" +
                    " interactive elements and how to audit your app for content label related " +
                    "improvements.",
            documentation = "https://developer.android.com/guide/topics/ui/accessibility/apps#describe-ui-element",
            apiSurface = AccessiblityApiSurface,
            content = { SpeakableText() },
        ),
        ComposableSampleDemo(
            id = "image-capture",
            name = "Image Capture",
            description = "This sample demonstrates how to capture an image using Camera2 and encode it " +
                    "into a JPEG container.",
            documentation = "https://developer.android.com/training/camera2/capture-sessions-requests",
            apiSurface = CameraCamera2ApiSurface,
            tags = listOf("Camera2"),
            content = { AndroidFragment<Camera2ImageCapture>() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-image-capture",
            name = "UltraHDR Image Capture",
            description = "This sample demonstrates how to capture a 10-bit compressed still image and " +
                    "store it using the new UltraHDR image format using Camera2.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = CameraCamera2ApiSurface,
            tags = listOf("UltraHDR", "Camera2"),
            content = { AndroidFragment<Camera2UltraHDRCapture>() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-image-capture",
            name = "Camera2 Preview",
            description = "Demonstrates displaying processed pixel data directly from the camera sensor "
                    + "to the screen using Camera2.",
            documentation = "https://developer.android.com/training/camera2",
            apiSurface = CameraCamera2ApiSurface,
            tags = listOf("Camera2"),
            content = { AndroidFragment<Camera2Preview>() },
        ),

        // CameraX Samples
        ComposableSampleDemo(
            id = "camerax-basic",
            name = "CameraX • Basic Image Capture",
            description = "This sample demonstrates how to capture an image using CameraX",
            documentation = "https://developer.android.com/training/camerax",
            apiSurface = CameraCameraXApiSurface,
            tags = listOf("CameraX"),
            content = { CameraXBasic() },
        ),
        ComposableSampleDemo(
            id = "camerax-video",
            name = "CameraX • Basic Video Capture",
            description = "This sample demonstrates how to capture an video using CameraX",
            documentation = "https://developer.android.com/training/camerax",
            apiSurface = CameraCameraXApiSurface,
            tags = listOf("CameraX"),
            content = { CameraXVideo() },
        ),
        ComposableSampleDemo(
            id = "camerax-extensions",
            name = "CameraX • Extensions",
            description = "This sample demonstrates how to check for and utilize CameraX Extensions",
            documentation = "https://developer.android.com/training/camerax",
            apiSurface = CameraCameraXApiSurface,
            tags = listOf("CameraX"),
            content = { CameraXExtensions() },
        ),
        ComposableSampleDemo(
            id = "camerax-mlkit",
            name = "CameraX • MLKit Sample",
            description = "This sample demonstrates using CameraX in conjunction with MLKit",
            documentation = "https://developer.android.com/training/camerax",
            apiSurface = CameraCameraXApiSurface,
            tags = listOf("CameraX"),
            content = { CameraXMlKitScreen() },
        ),

        ComposableSampleDemo(
            id = "communication-audio-manager",
            name = "Communication Audio Manager",
            description = "This sample shows how to use audio manager to for Communication application that self-manage the call.",
            documentation = "https://developer.android.com/guide/topics/connectivity/ble-audio/audio-manager",
            minSdk = Build.VERSION_CODES.S,
            apiSurface = ConnectivityAudioApiSurface,
            tags = listOf("Audio"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.S) {
                    //noinspection NewApi
                    AudioCommsSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "create-gatt-server",
            name = "Create a GATT server",
            description = "Shows how to create a GATT server and communicate with the GATT client",
            documentation = "https://developer.android.com/reference/android/bluetooth/BluetoothGattServer",
            apiSurface = ConnectivityBluetoothBleApiSurface,
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
                    GATTServerSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "scan-with-ble-intent",
            name = "Scan with BLE Intent",
            description = "This samples shows how to use the BLE intent to scan for devices",
            documentation = "https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(java.util.List%3Candroid.bluetooth.le.ScanFilter%3E,%20android.bluetooth.le.ScanSettings,%20android.app.PendingIntent)",
            apiSurface = ConnectivityBluetoothBleApiSurface,
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.O) {
                    //noinspection NewApi
                    BLEScanIntentSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "connect-gatt-server",
            name = "Connect to a GATT server",
            description = "Shows how to connect to a GATT server hosted by the BLE device and perform simple operations",
            documentation = "https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server",
            apiSurface = ConnectivityBluetoothBleApiSurface,
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
                    ConnectGATTSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "find-devices",
            name = "Find devices",
            description = "This example will demonstrate how to scanning for Low Energy Devices",
            documentation = "https://developer.android.com/guide/topics/connectivity/bluetooth",
            apiSurface = ConnectivityBluetoothBleApiSurface,
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
                    FindBLEDevicesSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "companion-device-manager",
            name = "Companion Device Manager",
            description = "This samples shows how to use the CDM to pair and connect with BLE devices",
            documentation = "https://developer.android.com/guide/topics/connectivity/companion-device-pairing",
            apiSurface = ConnectivityBluetoothCompanionApiSurface,
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.O) {
                    //noinspection NewApi
                    CompanionDeviceManagerSample()
                }
            },
        ),
        ActivitySampleDemo(
            id = "call-notification",
            name = "Call Notification",
            description = "Sample demonstrating how to make incoming call notifications and in call notifications",
            documentation = "https://developer.android.com/reference/android/app/Notification.CallStyle",
            apiSurface = ConnectivityCallNotificationApiSurface,
            content = CallNotificationSample::class.java,
        ),
        ComposableSampleDemo(
            id = "telecom-call",
            name = "Telecom Call",
            description = "A sample showcasing how to handle calls with the Jetpack Telecom API",
            documentation = "https://developer.android.com/guide/topics/connectivity/telecom",
            apiSurface = ConnectivityTelecomApiSurface,
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.O) {
                    //noinspection NewApi
                    TelecomCallSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "pdf-renderer",
            name = "PDF Renderer",
            description = "Demonstrates how to use PdfRenderer to display PDF documents on the screen.",
            documentation = null,
            apiSurface = GraphicsPdfApiSurface,
            content = { PdfRendererScreen() },
        ),
        ComposableSampleDemo(
            id = "compressing-ultrahdr-images",
            name = "Compressing UltraHDR Images",
            description = "This sample demonstrates displaying an UltraHDR image in a Compose View and an Android View",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = GraphicsUltraHdrApiSurface,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<CompressingUltraHDRImages>() },
        ),
        ComposableSampleDemo(
            id = "displaying-ultrahdr",
            name = "Displaying UltraHDR",
            description = "This sample demonstrates displaying an UltraHDR image.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = GraphicsUltraHdrApiSurface,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<DisplayingUltraHDR>() },
        ),
        ComposableSampleDemo(
            id = "displaying-ultrahdr",
            name = "Displaying UltraHDR (3P Libraries)",
            description = "This sample demonstrates using the various popular image loading library to" +
                    " detect the presence of a gainmap to enable HDR mode when displaying an UltraHDR image",
            documentation = "https://github.com/bumptech/glide",
            apiSurface = GraphicsUltraHdrApiSurface,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<DisplayingUltraHDRUsing3PLibrary>() },
        ),
        ComposableSampleDemo(
            id = "displaying-ultrahdr-compose",
            name = "Displaying UltraHDR (Compose)",
            description = "This sample demonstrates displaying an UltraHDR image in a Compose View and an Android View",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = GraphicsUltraHdrApiSurface,
            tags = listOf("UltraHDR", "Compose"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    //noinspection NewApi
                    DisplayUltraHDRScreen()
                }
            },
        ),
        ComposableSampleDemo(
            id = "visualizing-ultrahdr-gainmap",
            name = "Visualizing an UltraHDR Gainmap",
            description = "This sample demonstrates visualizing the underlying gainmap of an UltraHDR " +
                    "image, which reveals which parts of the image are enhanced by the gainmap.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = GraphicsUltraHdrApiSurface,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<VisualizingAnUltraHDRGainmap>() },
        ),
        ComposableSampleDemo(
            id = "editing-ultrahdr",
            name = "Editing UltraHDR",
            description = "This sample demonstrates editing an UltraHDR image and the resulting gainmap as well. Spatial edit operations like crop, rotate, scale are supported",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = GraphicsUltraHdrApiSurface,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<EditingUltraHDR>() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-opengles-surfaceview",
            name = "UltraHDR x OpenGLES SurfaceView",
            description = "This sample demonstrates displaying an UltraHDR image via and OpenGL Pipeline " +
                    "and control the SurfaceView's rendering brightness.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = GraphicsUltraHdrApiSurface,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<UltraHDRWithOpenGL>() },
        ),
        ComposableSampleDemo(
            id = "location-background-location-updates",
            name = "Location - Background Location updates",
            description = "This Sample demonstrate how to access location and get location updates when app is in background",
            documentation = "https://developer.android.com/training/location/background",
            apiSurface = LocationApiSurface,
            content = { BgLocationAccessScreen() },
        ),
        ComposableSampleDemo(
            id = "location-getting-current-location",
            name = "Location - Getting Current Location",
            description = "This Sample demonstrate how to request of current location",
            documentation = "https://developer.android.com/training/location/retrieve-current",
            apiSurface = LocationApiSurface,
            content = { CurrentLocationScreen() },
        ),
        ComposableSampleDemo(
            id = "location-create-monitor-geofence",
            name = "Location - Create and monitor Geofence",
            description = "This Sample demonstrate best practices for Creating and monitoring geofence",
            documentation = "https://developer.android.com/training/location/geofencing",
            apiSurface = LocationApiSurface,
            content = { GeofencingScreen() },
        ),
        ComposableSampleDemo(
            id = "location-updates",
            name = "Location - Updates",
            description = "This Sample demonstrate how to get location updates",
            documentation = "https://developer.android.com/training/location/request-updates",
            apiSurface = LocationApiSurface,
            content = { LocationUpdatesScreen() },
        ),
        ComposableSampleDemo(
            id = "location-permissions",
            name = "Location - Permissions",
            description = "This Sample demonstrate best practices for Location Permission",
            documentation = "https://developer.android.com/training/location/permissions",
            apiSurface = LocationApiSurface,
            tags = listOf("permissions"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.Q) {
                    //noinspection NewApi
                    LocationPermissionScreen()
                }
            },
        ),
        ComposableSampleDemo(
            id = "location-user-activity-recognition",
            name = "Location - User Activity Recognition",
            description = "This Sample demonstrate detection of user activity like walking, driving, etc.",
            documentation = "https://developer.android.com/training/location/transitions",
            apiSurface = LocationApiSurface,
            content = { UserActivityRecognitionScreen() },
        ),
        ComposableSampleDemo(
            id = "ultrahdr-to-hdr-video",
            name = "UltraHDR to HDR Video",
            description = "This sample demonstrates converting a series of UltraHDR images into a HDR " +
                    "video." + "The sample leverages GPU hardware acceleration to render and encode the " +
                    "images.",
            documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
            apiSurface = MediaUltraHdrApiSurface,
            tags = listOf("UltraHDR"),
            content = { AndroidFragment<UltraHDRToHDRVideo>() },
        ),
        ComposableSampleDemo(
            id = "transformer-tflite",
            name = "Transformer and TFLite",
            description = "This sample demonstrates using Transformer with TFLite/RTLite by applying a selected art style to a video.",
            documentation = "https://developer.android.com/guide/topics/media/transformer",
            apiSurface = MediaVideoApiSurface,
            tags = listOf("Transformer"),
            content = { AndroidFragment<TransformerTFLite>() },
        ),
        ComposableSampleDemo(
            id = "video-composition-using-media3-transformer",
            name = "Video Composition using Media3 Transformer",
            description = "This sample demonstrates concatenation of two video assets and an image using Media3 Transformer library.",
            documentation = "https://developer.android.com/guide/topics/media/transformer",
            apiSurface = MediaVideoApiSurface,
            tags = listOf("Transformer"),
            content = { AndroidFragment<TransformerVideoComposition>() },
        ),
        ComposableSampleDemo(
            id = "package-visibility",
            name = "Package Visibility",
            description = "A sample that showcase how the package visibility queries affects the available packages",
            documentation = "https://developer.android.com/training/package-visibility",
            apiSurface = PrivacyDataApiSurface,
            content = { PackageVisibility() },
        ),
        ComposableSampleDemo(
            id = "permissions-compose",
            name = "Permissions using Compose",
            description = "This sample showcases how to request permission using Accompanist in Compose",
            documentation = "https://google.github.io/accompanist/permissions/",
            apiSurface = PrivacyPermissionsApiSurface,
            tags = listOf("Permissions"),
            content = { ComposePermissions() },
        ),
        ComposableSampleDemo(
            id = "permissions-compose",
            name = "Multiple Permissions",
            description = "Shows the recommended flow to request multiple RELATED runtime permissions",
            documentation = "https://developer.android.com/training/permissions/requesting",
            apiSurface = PrivacyPermissionsApiSurface,
            tags = listOf("Permissions"),
            content = { AndroidFragment<MultiplePermissions>() },
        ),
        ComposableSampleDemo(
            id = "permissionless",
            name = "Permissionless",
            description = "This sample demonstrate how you can avoid requesting permission for certain actions by leveraging System APIs",
            documentation = "https://developer.android.com/training/permissions/evaluating",
            apiSurface = PrivacyPermissionsApiSurface,
            tags = listOf("Permissions"),
            content = { Permissionless() },
        ),
        ComposableSampleDemo(
            id = "single-permission",
            name = "Single Permission",
            description = "Shows the recommended flow to request single runtime permissions",
            documentation = "https://developer.android.com/training/permissions/requesting",
            apiSurface = PrivacyPermissionsApiSurface,
            tags = listOf("Permissions"),
            content = { AndroidFragment<SinglePermission>() },
        ),
        ComposableSampleDemo(
            id = "data-access",
            name = "Data Access",
            description = "Demonstrates how to implement data access auditing for your app to identify " +
                    "unexpected data access, even from third-party SDKs and libraries.",
            documentation = "https://developer.android.com/guide/topics/data/audit-access",
            apiSurface = PrivacyTransparencyApiSurface,
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.R) {
                    //noinspection NewApi
                    DataAccessSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "screenshot-detection",
            name = "Screenshot Detection",
            description = "This sample shows how to detect that the user capture the screen in Android 14 onwards",
            documentation = null,
            apiSurface = PrivacyTransparencyApiSurface,
            content = { AndroidFragment<ScreenshotDetectionSample>() },
        ),
        ComposableSampleDemo(
            id = "photo-picker",
            name = "PhotoPicker",
            description = "Select images/videos in a privacy-friendly way using the photo picker",
            documentation = "https://developer.android.com/training/data-storage/shared/photopicker",
            apiSurface = StorageApiSurface,
            content = { PhotoPickerSample() },
        ),
        ComposableSampleDemo(
            id = "mediastore-query",
            name = "MediaStore - Query",
            description = "Query files indexed by MediaStore",
            documentation = "https://developer.android.com/training/data-storage/shared/media#media_store",
            apiSurface = StorageApiSurface,
            content = { MediaStoreQuerySample() },
        ),
        ComposableSampleDemo(
            id = "selected-photos-access",
            name = "Selected Photos Access",
            description = "Check and request storage permissions",
            documentation = "https://developer.android.com/about/versions/14/changes/partial-photo-video-access",
            apiSurface = StorageApiSurface,
            content = { SelectedPhotosAccessSample() },
        ),
        ComposableSampleDemo(
            id = "app-widgets",
            name = "App Widgets",
            description = "Showcases how to pin widgets within the app and provides a catalog of well-designed canonical widget layouts for inspiration.",
            documentation = "https://developer.android.com/develop/ui/views/appwidgets/overview",
            apiSurface = UserInterfaceAppWidgetsApiSurface,
            tags = listOf("App Widgets"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.O) {
                    //noinspection NewApi
                    AppWidgets()
                }
            },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-centering-views",
            name = "ConstraintLayout - 1. Centering Views",
            description = "Center child views horizontally or vertically.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<CenteringViewsFragment>() },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-basic-arrangement",
            name = "ConstraintLayout - 2. Basic arrangement",
            description = "Arrange positions of child views relative to other views.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<BasicArrangementFragment>() },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-advanced-arrangement",
            name = "ConstraintLayout - 3. Advanced arrangement",
            description = "More arrangement options.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<AdvancedArrangementFragment>() },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-aspect-ratio",
            name = "ConstraintLayout - 4. Aspect ratio",
            description = "Specify aspect ratio for the dimensions of the child views.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<AspectRatioFragment>() },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-basic-chains",
            name = "ConstraintLayout - 5. Basic chains",
            description = "Use chains to arrange multiple child views horizontally or vertically.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<BasicChainFragment>() },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-advanced-chains",
            name = "ConstraintLayout - 5. Advanced chains",
            description = "Use chains to arrange multiple child views horizontally or vertically.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<AdvancedChainsFragment>() },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-constraintset",
            name = "ConstraintLayout - 7. ConstraintSet",
            description = "Use ConstraintSet to specify multiple constraints to all the child views.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<ConstraintSetFragment>() },
        ),
        ComposableSampleDemo(
            id = "constraintlayout-guidelines",
            name = "ConstraintLayout - 8. Guidelines",
            description = "Use a horizontal or vertical guideline to apply constraints to child views.",
            documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("ConstraintLayout"),
            content = { AndroidFragment<GuidelinesFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-01-basic",
            name = "MotionLayout - 01. Basic",
            description = "Basic motion example using referenced ConstraintLayout files",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<MotionBasic01Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-02-basic",
            name = "MotionLayout - 02. Basic",
            description = "Basic motion example using ConstraintSets defined in the MotionScene file",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<MotionBasic02Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-02-basic-no-auto-complete",
            name = "MotionLayout - 02. Basic, no auto complete",
            description = "Basic motion example same as 2, but autoComplete is set to false in onSwipe",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<MotionBasic02NoAutoCompleteFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-03-custom-attribute",
            name = "MotionLayout - 03. Custom attribute",
            description = "Show color interpolation (custom attribute)",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<CustomAttributeFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-04-imagefilterview-1",
            name = "MotionLayout - 04. ImageFilterView 1",
            description = "Show image cross-fade (using ML's ImageFilterView + custom attribute)",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ImageFilter1Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-05-imagefilterview-2",
            name = "MotionLayout - 05. ImageFilterView 2",
            description = "Show image saturation transition (using ML's ImageFilterView + custom attribute)",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ImageFilter2Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-06-keyframe-position",
            name = "MotionLayout - 06. Keyframe position",
            description = "Use a simple keyframe to change the interpolated motion",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<KeyframePositionFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-07-keyframe-interpolation",
            name = "MotionLayout - 07. Keyframe interpolation",
            description = "More complex keyframe, adding rotation interpolation",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<KeyframeInterpolationFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-08-keyframe-cycle",
            name = "MotionLayout - 08. Keyframe cycle",
            description = "Basic example of using a keyframe cycle",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<KeyframeCycleFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-09-coordinatorlayout-1",
            name = "MotionLayout - 09. CoordinatorLayout 1",
            description = "Basic example of using MotionLayout instead of AppBarLayout",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<Coordinator1Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-10-coordinatorlayout-2",
            name = "MotionLayout - 10. CoordinatorLayout 2",
            description = "Slightly more complex example of MotionLayout replacing AppBarLayout, with multiple elements and parallax background",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<Coordinator2Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-11-coordinatorlayout-3",
            name = "MotionLayout - 11. CoordinatorLayout 3",
            description = "Another AppBarLayout replacement example",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<Coordinator3Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-12-drawerlayout-1",
            name = "MotionLayout - 12. DrawerLayout 1",
            description = "Basic DrawerLayout with motionlayout",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<Drawer1Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-13-drawerlayout-2",
            name = "MotionLayout - 13. DrawerLayout 2",
            description = "Advanced DrawerLayout with motionlayout",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<Drawer2Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-14-sidepanel",
            name = "MotionLayout - 14. SidePanel",
            description = "Side Panel, implemented with MotionLayout only",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<SidePanelFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-15-parallax",
            name = "MotionLayout - 15. Parallax",
            description = "Parallax background. Drag the car.",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ParallaxFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-16-viewpager",
            name = "MotionLayout - 16. ViewPager",
            description = "Using MotionLayout with ViewPager",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ViewPagerFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-17-complex-motion-1",
            name = "MotionLayout - 17. Complex Motion 1",
            description = "Basic CoordinatorLayout-like behavior. Implemented with MotionLayout only, using a moving guideline. Note the view isn't resized.",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ComplexMotion1Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-18-complex-motion-2",
            name = "MotionLayout - 18. Complex Motion 2",
            description = "Advanced CoordinatorLayout-like behavior (adding a FAB). Implemented with MotionLayout only, using a moving guideline. Note the view isn't resized.",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ComplexMotion2Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-19-complex-motion-3",
            name = "MotionLayout - 19. Complex Motion 3",
            description = "Advanced CoordinatorLayout-like behavior (adding a FAB). Implemented with MotionLayout only, using direct resizing of the view.",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ComplexMotion3Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-20-complex-motion-4",
            name = "MotionLayout - 20. Complex Motion 4",
            description = "Advanced Synchronized reveal motion + helper (bounce). Implemented with MotionLayout only.",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<ComplexMotion4Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-21-fragment-transition-1",
            name = "MotionLayout - 21. Fragment transition 1",
            description = "Using MotionLayout with ViewPager",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<FragmentTransitionFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-22-fragment-transition-2",
            name = "MotionLayout - 22. Fragment transition 2",
            description = "Using MotionLayout with ViewPager",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<FragmentTransition2Fragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-23-lottie",
            name = "MotionLayout - 23. Lottie",
            description = "Using MotionLayout and Lottie with ViewPager",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<LottieFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-24-youtube-like-motion",
            name = "MotionLayout - 24. YouTube-like motion",
            description = "Example showing a transition like YouTube",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<YoutubeFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-25-keytrigger",
            name = "MotionLayout - 25. KeyTrigger",
            description = "Example that calls a method using KeyTrigger",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<KeyTriggerFragment>() },
        ),
        ComposableSampleDemo(
            id = "motionlayout-26-multi-state",
            name = "MotionLayout - 26. Multi-state",
            description = "Example that transitions between multiple states",
            documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
            apiSurface = UserInterfaceConstraintLayoutApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<MultiStateFragment>() },
        ),
        ComposableSampleDemo(
            id = "drag-and-drop-in-multiwindow-mode",
            name = "Drag and Drop in MultiWindow mode",
            description = "Drag and drop to another app visible in multiwindow mode",
            documentation = "https://developer.android.com/develop/ui/views/touch-and-input/drag-drop/multi-window",
            apiSurface = UserInterfaceDragAndDropApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<DragAndDropMultiWindow>() },
        ),
        ComposableSampleDemo(
            id = "drag-and-drop-richcontentreceiver",
            name = "Drag and Drop using the RichContentReceiver",
            description = "Using RichContentReceiverInterface for implementing Drop for rich data types",
            documentation = "https://developer.android.com/develop/ui/views/receive-rich-content",
            apiSurface = UserInterfaceDragAndDropApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<DragAndDropRichContentReceiverFragment>() },
        ),
        ComposableSampleDemo(
            id = "drag-and-drop-compose",
            name = "Drag and Drop in Compose",
            description = "Drag and drop in Compose",
            documentation = null,
            apiSurface = UserInterfaceDragAndDropApiSurface,
            tags = listOf("MotionLayout"),
            content = { DragAndDropCompose() },
        ),
        ComposableSampleDemo(
            id = "drag-and-drop-helper",
            name = "Drag and Drop - Helper",
            description = "Drag and Drop using the DragHelper and DropHelper from DragAndDropHelper library",
            documentation = "https://developer.android.com/develop/ui/views/touch-and-input/drag-drop#drophelper",
            apiSurface = UserInterfaceDragAndDropApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<DragAndDropWithHelper>() },
        ),
        ComposableSampleDemo(
            id = "drag-and-drop-views",
            name = "Drag and Drop using views",
            description = "Drag and Drop using the views",
            documentation = "https://developer.android.com/develop/ui/views/touch-and-input/drag-drop/view",
            apiSurface = UserInterfaceDragAndDropApiSurface,
            tags = listOf("MotionLayout"),
            content = { AndroidFragment<DragAndDropWithViews>() },
        ),
        ComposableSampleDemo(
            id = "haptics-1-vibration-effects",
            name = "Haptics - 1. Vibration effects",
            description = "Shows various vibration effects.",
            documentation = "https://source.android.com/docs/core/interaction/haptics",
            apiSurface = UserInterfaceHapticsApiSurface,
            tags = listOf("Haptics"),
            content = { HapticsBasic() },
        ),
        ComposableSampleDemo(
            id = "haptics-2-resist",
            name = "Haptics - 2. Resist",
            description = "Simulates resistance by increasing the intensity and reducing the duration between vibration effects.",
            documentation = "https://source.android.com/docs/core/interaction/haptics",
            apiSurface = UserInterfaceHapticsApiSurface,
            tags = listOf("Haptics"),
            content = { Resist() },
        ),
        ComposableSampleDemo(
            id = "haptics-3-expand",
            name = "Haptics - 3. Expand",
            description = "Expands and collapses a circle with haptics with an added tick to sharpen that the animation has ended.",
            documentation = "https://source.android.com/docs/core/interaction/haptics",
            apiSurface = UserInterfaceHapticsApiSurface,
            tags = listOf("Haptics"),
            content = { Expand() },
        ),
        ComposableSampleDemo(
            id = "haptics-4-bounce",
            name = "Haptics - 4. Bounce",
            description = "Play primitive effects to simulate physical interactions.",
            documentation = "https://source.android.com/docs/core/interaction/haptics",
            apiSurface = UserInterfaceHapticsApiSurface,
            tags = listOf("Haptics"),
            content = { Bounce() },
        ),
        ComposableSampleDemo(
            id = "haptics-5-wobble",
            name = "Haptics - 5. Wobble",
            description = "Play primitive effects to simulate physical interactions.",
            documentation = "https://source.android.com/docs/core/interaction/haptics",
            apiSurface = UserInterfaceHapticsApiSurface,
            tags = listOf("Haptics"),
            content = { Wobble() },
        ),
        ComposableSampleDemo(
            id = "live-updates",
            name = "Live Updates - ProgressStyle implementation",
            description = "Usage of ProgressStyle with Live update treatment",
            documentation = "https://developer.android.com/about/versions/16/features/progress-centric-notifications",
            minSdk = Build.VERSION_CODES.BAKLAVA,
            apiSurface = UserInterfaceLiveUpdatesApiSurface,
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.BAKLAVA) {
                    //noinspection NewApi
                    LiveUpdateSample()
                }
            },
        ),
        ActivitySampleDemo(
            id = "picture-in-picture-video-playback",
            name = "Picture in Picture (PiP) - Video playback",
            description = "Basic usage of Picture-in-Picture mode showcasing video playback",
            documentation = "https://developer.android.com/develop/ui/views/picture-in-picture",
            apiSurface = UserInterfacePictureInPictureApiSurface,
            content = PiPMovieActivity::class.java,
        ),
        ActivitySampleDemo(
            id = "picture-in-picture-stopwatch",
            name = "Picture in Picture (PiP) - Stopwatch",
            description = "Basic usage of Picture-in-Picture mode showcasing a stopwatch",
            documentation = "https://developer.android.com/develop/ui/views/picture-in-picture",
            apiSurface = UserInterfacePictureInPictureApiSurface,
            content = PiPSampleActivity::class.java,
        ),
        ActivitySampleDemo(
            id = "predictive-back",
            name = "Predictive Back",
            description = "Shows Predictive Back animations.",
            documentation = "https://developer.android.com/about/versions/14/features/predictive-back",
            apiSurface = UserInterfacePredictiveBackApiSurface,
            content = PBHostingActivity::class.java,
        ),
        ComposableSampleDemo(
            id = "quick-settings",
            name = "Quick Settings",
            description = "Add your custom tile to the Quick Settings.",
            documentation = "https://developer.android.com/develop/ui/views/quicksettings-tiles",
            apiSurface = UserInterfaceQuickSettingsApiSurface,
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.N) {
                    //noinspection NewApi
                    QuickSettings()
                }
            },
        ),
        ActivitySampleDemo(
            id = "receive-data-shared-by-other-apps",
            name = "Receive data shared by other apps",
            description = "Receive texts and images from other apps.",
            documentation = null,
            apiSurface = UserInterfaceShareApiSurface,
            content = ShareReceiverActivity::class.java,
        ),
        ComposableSampleDemo(
            id = "send-data-with-sharesheet",
            name = "Send data with sharesheet",
            description = "Send texts and images to other apps using the Android Sharesheet.",
            documentation = null,
            apiSurface = UserInterfaceShareApiSurface,
            content = { ShareSender() },
        ),
        ComposableSampleDemo(
            id = "conversion-suggestions",
            name = "Conversion suggestions",
            description = "Demonstrates how to implement the incremental search feature for non-alphabet languages with the Conversion Suggestions API.",
            documentation = "https://developer.android.com/about/versions/13/features#text-conversion",
            apiSurface = UserInterfaceTextApiSurface,
            tags = listOf("Text"),
            content = { AndroidFragment<ConversionSuggestions>() },
        ),
        ComposableSampleDemo(
            id = "downloadable-fonts",
            name = "Downloadable Fonts",
            description = "Download fonts instead of bundling them in the app resources.",
            documentation = "https://developer.android.com/develop/ui/views/text-and-emoji/downloadable-fonts",
            apiSurface = UserInterfaceTextApiSurface,
            tags = listOf("Text"),
            content = { AndroidFragment<DownloadableFontsFragment>() },
        ),
        ComposableSampleDemo(
            id = "hyphenation",
            name = "Hyphenation",
            description = "Demonstrates different options for the `android:hyphenationFrequency` attribute",
            documentation = "https://developer.android.com/reference/android/widget/TextView#attr_android:hyphenationFrequency",
            apiSurface = UserInterfaceTextApiSurface,
            tags = listOf("Text"),
            content = { AndroidFragment<Hyphenation>() },
        ),
        ComposableSampleDemo(
            id = "line-break",
            name = "LineBreak",
            description = "Demonstrates different options for the `android:lineBreakWordStyle` attribute.",
            documentation = "https://developer.android.com/about/versions/13/features#japanese-wrapping",
            apiSurface = UserInterfaceTextApiSurface,
            tags = listOf("Text"),
            content = { AndroidFragment<LineBreak>() },
        ),
        ComposableSampleDemo(
            id = "linkify",
            name = "Linkify",
            description = "Linkify is useful for creating links in TextViews.",
            documentation = "https://developer.android.com/reference/kotlin/androidx/core/text/util/LinkifyCompat",
            apiSurface = UserInterfaceTextApiSurface,
            tags = listOf("Text"),
            content = { AndroidFragment<Linkify>() },
        ),
        ComposableSampleDemo(
            id = "text-span",
            name = "TextSpan",
            description = "buildSpannedString is useful for quickly building a rich text.",
            documentation = "https://developer.android.com/kotlin/ktx#core",
            apiSurface = UserInterfaceTextApiSurface,
            tags = listOf("Text"),
            content = { AndroidFragment<TextSpanFragment>() },
        ),
        ComposableSampleDemo(
            id = "immersive-mode",
            name = "Immersive mode",
            description = "Immersive mode enables your app to display full-screen by hiding system bars.",
            documentation = "https://developer.android.com/develop/ui/views/layout/immersive",
            apiSurface = UserInterfaceWindowInsetsApiSurface,
            content = { ImmersiveMode() },
        ),
        ActivitySampleDemo(
            id = "window-insets-animation",
            name = "WindowInsetsAnimation",
            description = "Shows how to react to the on-screen keyboard (IME) changing visibility, and also controlling the IME's visibility.",
            documentation = "https://developer.android.com/develop/ui/views/layout/sw-keyboard",
            apiSurface = UserInterfaceWindowInsetsApiSurface,
            content = WindowInsetsAnimationActivity::class.java,
        ),
        ActivitySampleDemo(
            id = "window-manager",
            name = "WindowManager",
            description = "Demonstrates how to use the Jetpack WindowManager library.",
            documentation = "https://developer.android.com/jetpack/androidx/releases/window",
            apiSurface = UserInterfaceWindowManagerApiSurface,
            content = WindowDemosActivity::class.java,
        ),
    ).associateBy { it.id }
}