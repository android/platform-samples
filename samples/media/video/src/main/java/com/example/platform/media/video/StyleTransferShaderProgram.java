/*
 * Copyright 2024 The Android Open Source Project
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
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.GlProgram;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Size;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.BaseGlShaderProgram;

import com.google.common.collect.ImmutableMap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.opengles.GL10;

// TODO: Migrate this class to Kotlin
@UnstableApi
final class StyleTransferShaderProgram extends BaseGlShaderProgram {

    private static final String TAG = "StyleTransferSP";
    private static final String VERTEX_SHADER_PATH = "shaders/vertex_shader_transformation_es2.glsl";
    private static final String FRAGMENT_SHADER_PATH = "shaders/fragment_shader_copy_es2.glsl";

    private final GlProgram glProgram;
    private final InterpreterApi transformInterpreter;
    private final int inputTransformTargetHeight;
    private final int inputTransformTargetWidth;
    private final int[] outputTransformShape;

    private final TensorBuffer predictOutput;

    private int width;
    private int height;

    public StyleTransferShaderProgram(Context context, String styleAssetFileName)
            throws VideoFrameProcessingException {
        super(/* useHighPrecisionColorComponents= */ false, /* texturePoolCapacity= */ 1);

        try {
            glProgram = new GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);

            Interpreter.Options options = new Interpreter.Options();

            CompatibilityList compatibilityList = new CompatibilityList();
            if (compatibilityList.isDelegateSupportedOnThisDevice()) {
                GpuDelegate.Options gpuDelegateOptions = compatibilityList.getBestOptionsForThisDevice();
                GpuDelegate gpuDelegate = new GpuDelegate(gpuDelegateOptions);
                options.addDelegate(gpuDelegate);
            } else {
                options.setNumThreads(6);
            }
            String predictModel = "predict_float16.tflite";
            String transferModel = "transfer_float16.tflite";
            Interpreter predictInterpeter =
                    new Interpreter(FileUtil.loadMappedFile(context, predictModel), options);
            transformInterpreter =
                    InterpreterApi.create(FileUtil.loadMappedFile(context, transferModel), options);
            int inputPredictTargetHeight = predictInterpeter.getInputTensor(0).shape()[1];
            int inputPredictTargetWidth = predictInterpeter.getInputTensor(0).shape()[2];
            int[] outputPredictShape = predictInterpeter.getOutputTensor(0).shape();

            inputTransformTargetHeight = transformInterpreter.getInputTensor(0).shape()[1];
            inputTransformTargetWidth = transformInterpreter.getInputTensor(0).shape()[2];
            outputTransformShape = transformInterpreter.getOutputTensor(0).shape();

            InputStream inputStream = context.getAssets().open(styleAssetFileName);
            Bitmap styleImage = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            TensorImage styleTensorImage =
                    getScaledTensorImage(styleImage, inputPredictTargetWidth, inputPredictTargetHeight);
            predictOutput = TensorBuffer.createFixedSize(outputPredictShape, DataType.FLOAT32);
            predictInterpeter.run(styleTensorImage.getBuffer(), predictOutput.getBuffer());
        } catch (IOException | GlUtil.GlException e) {
            Log.w(TAG, "Error setting up TfShaderProgram", e);
            throw new VideoFrameProcessingException(e);
        }
    }

    @Override
    public Size configure(int inputWidth, int inputHeight) {
        width = inputWidth;
        height = inputHeight;
        return new Size(inputWidth, inputHeight);
    }

    @Override
    public void drawFrame(int inputTexId, long presentationTimeUs)
            throws VideoFrameProcessingException {
        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);

        Bitmap bitmap;
        int texId;
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

            Log.w(TAG, "Process frame at " + (presentationTimeUs / 1000) + " ms");
            long before = System.currentTimeMillis();
            TensorImage tensorImage =
                    getScaledTensorImage(bitmap, inputTransformTargetWidth, inputTransformTargetHeight);
            Log.w(TAG, "- Scale " + (System.currentTimeMillis() - before) + " ms");
            TensorBuffer outputImage =
                    TensorBuffer.createFixedSize(outputTransformShape, DataType.FLOAT32);

            before = System.currentTimeMillis();
            transformInterpreter.runForMultipleInputsOutputs(
                    new Object[] {tensorImage.getBuffer(), predictOutput.getBuffer()},
                    ImmutableMap.<Integer, Object>builder().put(0, outputImage.getBuffer()).build());

            Log.w(TAG, "- Run " + (System.currentTimeMillis() - before) + " ms");

            before = System.currentTimeMillis();
            ImageProcessor imagePostProcessor =
                    new ImageProcessor.Builder()
                            .add(new DequantizeOp(/* zeroPoint= */ 0f, /* scale= */ 255f))
                            .build();
            TensorImage outputTensorImage = new TensorImage(DataType.FLOAT32);
            outputTensorImage.load(outputImage);
            Log.w(TAG, "- Load output " + (System.currentTimeMillis() - before) + " ms");

            before = System.currentTimeMillis();
            Bitmap outputBitmap = imagePostProcessor.process(outputTensorImage).getBitmap();
            Log.w(TAG, "- Post process output " + (System.currentTimeMillis() - before) + " ms");

            texId =
                    GlUtil.createTexture(
                            outputBitmap.getWidth(),
                            outputBitmap.getHeight(),
                            /* useHighPrecisionColorComponents= */ false);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
            GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, /* level= */ 0, outputBitmap, /* border= */ 0);
            GlUtil.checkGlError();

            GlUtil.focusFramebufferUsingCurrentContext(boundFramebuffer[0], width, height);

            glProgram.use();
            glProgram.setSamplerTexIdUniform("uTexSampler", texId, /* texUnitIndex= */ 0);
            float[] identityMatrix = GlUtil.create4x4IdentityMatrix();
            glProgram.setFloatsUniform("uTexTransformationMatrix", identityMatrix);
            glProgram.setFloatsUniform("uTransformationMatrix", identityMatrix);
            glProgram.setBufferAttribute(
                    "aFramePosition",
                    GlUtil.getNormalizedCoordinateBounds(),
                    GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
            glProgram.bindAttributesAndUniforms();

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4);
            GlUtil.checkGlError();

            GlUtil.deleteTexture(texId);
        } catch (GlUtil.GlException e) {
            throw VideoFrameProcessingException.from(e);
        }
    }

    private static TensorImage getScaledTensorImage(
            Bitmap bitmap, int targetWidth, int targetHeight) {
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(
                                new ResizeOp(
                                        targetHeight,
                                        targetWidth,
                                        ResizeOp.ResizeMethod.BILINEAR)) // TODO: Not sure why they are swapped?
                        .add(new NormalizeOp(/* mean= */ 0f, /* stddev= */ 255f))
                        .build();
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        return imageProcessor.process(tensorImage);
    }
}
