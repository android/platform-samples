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
import com.example.platform.connectivity.callnotification.CallNotificationSample
import com.example.platform.graphics.pdf.PdfRendererScreen
import com.example.platform.graphics.ultrahdr.display.CompressingUltraHDRImages
import com.example.platform.graphics.ultrahdr.display.DisplayUltraHDRScreen
import com.example.platform.graphics.ultrahdr.display.DisplayingUltraHDR
import com.example.platform.graphics.ultrahdr.display.DisplayingUltraHDRUsing3PLibrary
import com.example.platform.graphics.ultrahdr.display.VisualizingAnUltraHDRGainmap
import com.example.platform.graphics.ultrahdr.edit.EditingUltraHDR
import com.example.platform.graphics.ultrahdr.opengl.UltraHDRWithOpenGL
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
        ActivitySampleDemo(
            id = "call-notification",
            name = "Call Notification",
            description = "Sample demonstrating how to make incoming call notifications and in call notifications",
            documentation = "https://developer.android.com/reference/android/app/Notification.CallStyle",
            area = ConnectivityCallNotificationArea,
            content = CallNotificationSample::class.java,
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