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
import android.graphics.Bitmap;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Size;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.BaseGlShaderProgram;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector;

import java.nio.ByteBuffer;

@UnstableApi
public class TestGLShaderProgram extends BaseGlShaderProgram {

//    private final ObjectDetector objectDetector;

    private int width;
    private int height;


    static {
        System.loadLibrary("mediapipe_tasks_vision_jni");
    }


    public TestGLShaderProgram(Context context) {
        super(false, 1);

        BaseOptions baseOptions =
                BaseOptions.builder()
                        .setDelegate(Delegate.CPU)
                        .setModelAssetPath("efficientdet_lite0.tflite")
                        .build();

//        ObjectDetector.ObjectDetectorOptions objectDetectorOptions = ObjectDetector.ObjectDetectorOptions.builder()
//                .setBaseOptions(baseOptions)
//                .setScoreThreshold(0.5f)
//                .setMaxResults(1)
//                .setRunningMode(RunningMode.VIDEO)
//                .setErrorListener(e -> Log.w("DEBUG", "Error from media pipe", e))
//                .setResultListener((result, inout) -> {
//                    Log.w("DEBUG", "Result: " + result);
//                })
//                .build();
//        objectDetector = ObjectDetector.createFromOptions(context, objectDetectorOptions);
    }

    @NonNull
    @Override
    public Size configure(int inputWidth, int inputHeight) throws VideoFrameProcessingException {

        width = inputWidth;
        height = inputHeight;

        return new Size(inputWidth, inputHeight);
    }

    @Override
    public void drawFrame(int inputTexId, long presentationTimeUs) throws VideoFrameProcessingException {
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

            MPImage mpImage = new BitmapImageBuilder(bitmap).build();

            GlUtil.focusFramebufferUsingCurrentContext(boundFramebuffer[0], width, height);

//            objectDetector.detectAsync(mpImage, ImageProcessingOptions.builder().setRotationDegrees(180).build(), presentationTimeUs / 1000);
        } catch (GlUtil.GlException e) {
            onError(e);
        }
    }
}
