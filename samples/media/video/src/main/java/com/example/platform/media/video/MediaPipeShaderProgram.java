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

package com.example.platform.media.video;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.OptIn;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Size;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.BaseGlShaderProgram;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.effect.TextOverlay;

import com.google.common.collect.ImmutableList;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Runs a MediaPipe graph on input frames.
 */
/* package */
@UnstableApi
final class MediaPipeShaderProgram extends BaseGlShaderProgram {

    private static final String TAG = "MediaPipeShaderProgram";

    private ObjectDetector objectDetector;
    private final BaseGlShaderProgram overlayShaderProgram;

    private int width;
    private int height;

    private static final class OverlayInfo {
        public String description;
        public float xOffset;
        public float yOffset;
        public float scale = 1;
    }

    private static final int MAX_OVERLAYS = 1;
    private final OverlayInfo[] overlayInfos;

    private Bitmap overlay;
    private float overlayToInputScaleX = -1;
    private float overlayToInputScaleY = -1;

    @OptIn(markerClass = UnstableApi.class)
    public MediaPipeShaderProgram(Context context) throws VideoFrameProcessingException {
        super(/* useHighPrecisionColorComponents= */ false, /* texturePoolCapacity= */ 1);
        BaseOptions baseOptions =
                BaseOptions.builder()
                        .setDelegate(Delegate.CPU)
                        .setModelAssetPath("efficientdet_lite0.tflite")
                        .build();

        overlayInfos = new OverlayInfo[MAX_OVERLAYS];
        for (int i = 0; i < MAX_OVERLAYS; i++) {
            overlayInfos[i] = new OverlayInfo();
        }

        try {
            ObjectDetector.ObjectDetectorOptions objectDetectorOptions = ObjectDetector.ObjectDetectorOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setScoreThreshold(0.5f)
                    .setMaxResults(MAX_OVERLAYS)
                    .setRunningMode(RunningMode.VIDEO)
                    .setErrorListener(e -> Log.w("DEBUG", "Error from media pipe", e))
//                .setResultListener((result, inout) -> {
//                    Log.w("DEBUG", "Result: " + result);
//                })
                    .build();
            objectDetector = ObjectDetector.createFromOptions(context, objectDetectorOptions);
        } catch (Exception e) {
            Log.e("Caren", "Caught exception");
            e.printStackTrace();
        }


        TextOverlay[] textOverlays = new TextOverlay[MAX_OVERLAYS];
        for (int i = 0; i < MAX_OVERLAYS; i++) {
            OverlayInfo overlayInfo = new OverlayInfo();
            overlayInfos[i] = overlayInfo;
            overlayInfo.description = " ";
            textOverlays[i] = new TextOverlay() {
                @Override
                public SpannableString getText(long presentationTimeUs) {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(overlayInfo.description);
                    ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.WHITE);
                    //RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(0.5f);
                    spannableStringBuilder.setSpan(foregroundColorSpan, 0, spannableStringBuilder.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    //spannableStringBuilder.setSpan(relativeSizeSpan, 0, spannableStringBuilder.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    return SpannableString.valueOf(spannableStringBuilder);
                }

                @Override
                public float[] getVertexTransformation(long presentationTimeUs) {
                    float[] temp = GlUtil.create4x4IdentityMatrix();
                    Matrix.translateM(temp, 0, overlayInfo.xOffset, overlayInfo.yOffset, 0);
                    Matrix.scaleM(temp, /* offset */ 0, /* x= */ 1f, /* y= */ -1f, /* z= */ 1f);
                    return temp;
                }
            };
        }

        overlayShaderProgram = new OverlayEffect(ImmutableList.copyOf(textOverlays)).toGlShaderProgram(context, false);
    }


    @OptIn(markerClass = UnstableApi.class)
    @Override
    public Size configure(int inputWidth, int inputHeight) throws VideoFrameProcessingException {
        width = inputWidth;
        height = inputHeight;
        overlayShaderProgram.configure(inputWidth, inputHeight);
        overlayToInputScaleX = (float) width / overlay.getWidth();
        overlayToInputScaleY = (float) height / overlay.getHeight();
        return new Size(inputWidth, inputHeight);
    }

    @Override
    public void drawFrame(int inputTexId, long presentationTimeUs)
            throws VideoFrameProcessingException {

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4);

        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);
        Bitmap bitmap;
        try {
            int[] boundFramebuffer = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, boundFramebuffer, /* offset= */ 0);
            int fboId = GlUtil.createFboForTexture(inputTexId);
            GlUtil.focusFramebufferUsingCurrentContext(fboId, width, height);
            GLES20.glReadPixels(
                    /* x= */ 0,
                    /* y= */ 0,
                    width,
                    height,
                    GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE,
                    pixelBuffer);
            GlUtil.checkGlError();
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(pixelBuffer);

            //Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 320, 320, true);
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();

//            objectDetector.detectAsync(mpImage, ImageProcessingOptions.builder().setRotationDegrees(180).build(), presentationTimeUs / 1000);
            ObjectDetectorResult result = objectDetector.detectForVideo(mpImage, ImageProcessingOptions.builder().setRotationDegrees(180).build(), presentationTimeUs / 1000);
            updateObjectDetection(result);
            //scaledBitmap.recycle();
            bitmap.recycle();

            GlUtil.focusFramebufferUsingCurrentContext(boundFramebuffer[0], width, height);
            overlayShaderProgram.drawFrame(inputTexId, presentationTimeUs);
        } catch (GlUtil.GlException e) {
            onError(e);
        }
    }

    @Override
    public void release() throws VideoFrameProcessingException {
        overlayShaderProgram.release();
        super.release();
    }

    private void updateObjectDetection(ObjectDetectorResult result) {
        Log.w("DEBUG", "Result " + result);
        int index = 0;
        for (Detection detection : result.detections()) {
            RectF boundingBox = detection.boundingBox();
            overlayInfos[index].xOffset = -7 * (boundingBox.centerX() - width / 2f) / (width / 2f);
            overlayInfos[index].yOffset = 7 * (boundingBox.centerY() - height / 2f) / (height / 2f);
            overlayInfos[index].description = detection.categories().get(0).categoryName();
            index++;
        }
        while (index < MAX_OVERLAYS) {
            overlayInfos[index].description = " ";
            index++;
        }
    }

}
