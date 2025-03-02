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

package com.example.platform.media.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.media3.common.GlTextureInfo
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlRect
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.effect.ByteBufferGlEffect
import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.DequantizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.concurrent.Future

@UnstableApi
class StyleTransferEffect(context: Context, styleAssetFileName: String) : ByteBufferGlEffect.Processor<Bitmap> {

    private val transformInterpreter: InterpreterApi
    private val inputTransformTargetHeight: Int
    private val inputTransformTargetWidth: Int
    private val outputTransformShape: IntArray

    private var preProcess: ListeningExecutorService = MoreExecutors.listeningDecorator(
        Util.newSingleThreadExecutor("preProcess"))
    private var postProcess: ListeningExecutorService = MoreExecutors.listeningDecorator(
        Util.newSingleThreadExecutor("postProcess"))
    private var tfRun: ListeningExecutorService = MoreExecutors.listeningDecorator(
        Util.newSingleThreadExecutor("tfRun"))

    private val predictOutput: TensorBuffer

    private var inputWidth: Int = 0
    private var inputHeight: Int = 0


    init {
        val options = Interpreter.Options()
        val compatibilityList = CompatibilityList()
        val gpuDelegateOptions = compatibilityList.bestOptionsForThisDevice
        val gpuDelegate = GpuDelegate(gpuDelegateOptions)
        options.addDelegate(gpuDelegate)
        val predictModel = "predict_float16.tflite"
        val transferModel = "transfer_float16.tflite"
        val predictInterpreter = Interpreter(FileUtil.loadMappedFile(context, predictModel), options)
        transformInterpreter = InterpreterApi.create(FileUtil.loadMappedFile(context, transferModel), options)
        val inputPredictTargetHeight = predictInterpreter.getInputTensor(0).shape()[1]
        val inputPredictTargetWidth = predictInterpreter.getInputTensor(0).shape()[2]
        val outputPredictShape = predictInterpreter.getOutputTensor(0).shape()

        inputTransformTargetHeight = transformInterpreter.getInputTensor(0).shape()[1]
        inputTransformTargetWidth = transformInterpreter.getInputTensor(0).shape()[2]
        outputTransformShape = transformInterpreter.getOutputTensor(0).shape()

        val inputStream = context.assets.open(styleAssetFileName)
        val styleImage = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        val styleTensorImage = getScaledTensorImage(styleImage, inputPredictTargetWidth, inputPredictTargetHeight)
        predictOutput = TensorBuffer.createFixedSize(outputPredictShape, DataType.FLOAT32)
        predictInterpreter.run(styleTensorImage.buffer, predictOutput.buffer)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        this.inputWidth = inputWidth
        this.inputHeight = inputHeight
        return Size(inputTransformTargetWidth, inputTransformTargetHeight)
    }

    override fun getScaledRegion(presentationTimeUs: Long): GlRect {
        val minSide = minOf(inputWidth, inputHeight)
        return GlRect(0, 0, minSide, minSide)
    }

    override fun processImage(
        image: ByteBufferGlEffect.Image,
        presentationTimeUs: Long,
    ): ListenableFuture<Bitmap> {
        val tensorImageFuture = preProcess(image)
        val tensorBufferFuture = tfRun(tensorImageFuture)
        return postProcess(tensorBufferFuture)
    }

    override fun release() {}

    override fun finishProcessingAndBlend(
        outputFrame: GlTextureInfo,
        presentationTimeUs: Long,
        result: Bitmap,
    ) {
        try {
            copyBitmapToFbo(result, outputFrame, getScaledRegion(presentationTimeUs))
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException.from(e)
        }
    }

    private fun preProcess(image: ByteBufferGlEffect.Image): ListenableFuture<TensorImage> {
        return preProcess.submit<TensorImage> {
            val bitmap = image.copyToBitmap()
            getScaledTensorImage(bitmap, inputTransformTargetWidth, inputTransformTargetHeight)
        }
    }

    private fun tfRun(tensorImageFuture: Future<TensorImage>): ListenableFuture<TensorBuffer> {
        return tfRun.submit<TensorBuffer> {
            val tensorImage = tensorImageFuture.get()
            val outputImage = TensorBuffer.createFixedSize(outputTransformShape, DataType.FLOAT32)

            transformInterpreter.runForMultipleInputsOutputs(
                arrayOf(tensorImage.buffer, predictOutput.buffer),
                ImmutableMap.builder<Int, Any>().put(0, outputImage.buffer).build()
            )
            outputImage
        }
    }

    private fun postProcess(futureOutputImage: ListenableFuture<TensorBuffer>): ListenableFuture<Bitmap> {
        return postProcess.submit<Bitmap> {
            val outputImage = futureOutputImage.get()
            val imagePostProcessor = ImageProcessor.Builder()
                .add(DequantizeOp(0f, 255f))
                .build()
            val outputTensorImage = TensorImage(DataType.FLOAT32)
            outputTensorImage.load(outputImage)
            imagePostProcessor.process(outputTensorImage).bitmap
        }
    }

    private fun getScaledTensorImage(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): TensorImage {
        val cropSize = minOf(bitmap.width, bitmap.height)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    private fun copyBitmapToFbo(bitmap: Bitmap, textureInfo: GlTextureInfo, rect: GlRect) {
        val bitmapToGl = Matrix().apply { setScale(1f, -1f) }
        val texId = GlUtil.createTexture(bitmap.width, bitmap.height, false)
        val fboId = GlUtil.createFboForTexture(texId)
        GlUtil.setTexture(texId,
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, bitmapToGl, true))
        GlUtil.blitFrameBuffer(fboId, GlRect(0, 0, bitmap.width, bitmap.height), textureInfo.fboId, rect)
        GlUtil.deleteTexture(texId)
        GlUtil.deleteFbo(fboId)
    }
}