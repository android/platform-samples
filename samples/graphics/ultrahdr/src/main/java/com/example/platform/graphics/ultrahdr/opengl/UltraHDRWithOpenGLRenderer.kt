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
package com.example.platform.graphics.ultrahdr.opengl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class UltraHDRWithOpenGLRenderer(
    context: Context,
    private val bitmap: Bitmap,
) {

    private val triangleVertices: FloatBuffer =
        ByteBuffer.allocateDirect(triangleVerticesData.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

    private val mvpMatrix = FloatArray(16)
    private val orthoMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val mMatrix = FloatArray(16)
    private val vMatrix = FloatArray(16)
    private val destTF = FloatArray(7)

    private var program = 0
    private var textureID = 0
    private var gainmapTextureID = 0
    private var muMVPMatrixHandle = 0
    private var maPositionHandle = 0
    private var maTextureHandle = 0
    private var destTFHandle = 0
    private var wHandle = 0
    private var displayRatioSdr = 0f
    private var displayRatioHdr = 0f

    val desiredHdrSdrRatio get() = bitmap.gainmap?.displayRatioForFullHdr ?: 1f

    /**
     * Location of UltraHDR vertex shader program.
     */
    private var ultraHDRVertexShader: String = context.assets
        ?.open(FILE_ULTRAHDR_VERTEX_SHADER)
        ?.bufferedReader().use { it?.readText() ?: "" }

    /**
     * Location of UltraHDR fragment shader program.
     */
    private var ultraHDRFragmentShader: String = context.assets
        ?.open(FILE_ULTRAHDR_FRAGMENT_SHADER)
        ?.bufferedReader().use { it?.readText() ?: "" }

    init {
        triangleVertices.put(triangleVerticesData).position(0)
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = glCreateShader(shaderType)
        if (shader != 0) {
            glShaderSource(shader, source)
            glCompileShader(shader)
            val compiled = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                glDeleteShader(shader)
                shader = 0
            }
        }
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0

        val pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) return 0

        var program = glCreateProgram()
        if (program == 0) return 0

        glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        glAttachShader(program, pixelShader)
        checkGlError("glAttachShader")

        glLinkProgram(program)
        val linkStatus = IntArray(1)
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] != GL_TRUE) {
            glDeleteProgram(program)
            program = 0
        }

        return program
    }

    private fun checkGlError(op: String) {
        var error: Int
        while (glGetError().also { error = it } != GL_NO_ERROR)
            throw RuntimeException("$op: glError $error")
    }

    private val kSRGB = floatArrayOf(
        2.4f,
        (1 / 1.055).toFloat(),
        (0.055 / 1.055).toFloat(),
        (1 / 12.92).toFloat(),
        0.04045f,
        0.0f,
        0.0f,
    )

    private fun trfnApplyGain(trfn: FloatArray, gain: Float, dest: FloatArray) {
        val powGainGinv = gain.toDouble().pow(1.0 / trfn[0]).toFloat()
        dest[0] = trfn[0]
        dest[1] = trfn[1] * powGainGinv
        dest[2] = trfn[2] * powGainGinv
        dest[3] = trfn[3] * gain
        dest[4] = trfn[4]
        dest[5] = trfn[5] * gain
        dest[6] = trfn[6] * gain
    }

    fun mul3x3(lhs: FloatArray, rhs: FloatArray): FloatArray {
        val r = FloatArray(9)
        r[0] = lhs[0] * rhs[0] + lhs[3] * rhs[1] + lhs[6] * rhs[2]
        r[1] = lhs[1] * rhs[0] + lhs[4] * rhs[1] + lhs[7] * rhs[2]
        r[2] = lhs[2] * rhs[0] + lhs[5] * rhs[1] + lhs[8] * rhs[2]
        r[3] = lhs[0] * rhs[3] + lhs[3] * rhs[4] + lhs[6] * rhs[5]
        r[4] = lhs[1] * rhs[3] + lhs[4] * rhs[4] + lhs[7] * rhs[5]
        r[5] = lhs[2] * rhs[3] + lhs[5] * rhs[4] + lhs[8] * rhs[5]
        r[6] = lhs[0] * rhs[6] + lhs[3] * rhs[7] + lhs[6] * rhs[8]
        r[7] = lhs[1] * rhs[6] + lhs[4] * rhs[7] + lhs[7] * rhs[8]
        r[8] = lhs[2] * rhs[6] + lhs[5] * rhs[7] + lhs[8] * rhs[8]
        return r
    }

    private fun skcmsTransferfunctionEval(tf: FloatArray, x: Float): Float {
        val sign = if (x < 0) -1.0f else 1.0f
        val nx = x * sign
        return sign * if (nx < tf[4]) {
            tf[3] * nx * tf[6]
        } else {
            (tf[1] * nx + tf[2]).toDouble().pow(tf[0].toDouble()).toFloat() + tf[5]
        }
    }

    /**
     * This functions calculates the inverse function by finding the inverse of each segment and
     * then stitching the segments together. The code also checks to make sure that the inverse
     * function is well-defined and that it satisfies the same constraints as the original function.
     *
     * Finally, the code tweaks the inverse function to make sure that it satisfies the invariant
     * inv(src(1.0f)) == 1.0f.
     */
    private fun invertTrfn(src: FloatArray, dest: FloatArray) {
        // We're inverting this function, solving for x in terms of y.
        //   y = (cx + f)         x < d
        //       (ax + b)^g + e   x ≥ d
        // The inverse of this function can be expressed in the same piecewise form.
        val inv = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)

        // We'll start by finding the new threshold inv[4].
        // In principle we should be able to find that by solving for y at x=d from either side.
        // (If those two d values aren't the same, it's a discontinuous transfer function.)
        val dl = src[3] * src[4] + src[6]
        val dr = (src[1] * src[4] + src[2]).toDouble().pow(src[0].toDouble()).toFloat() + src[5]
        if (abs(dl - dr) > 1 / 512.0f) throw IllegalArgumentException()
        inv[4] = dl

        // When d=0, the linear section collapses to a point.  We leave c,d,f all zero in that case.
        if (inv[4] > 0) {
            // Inverting the linear section is pretty straightforward:
            //        y       = cx + f
            //        y - f   = cx
            //   (1/c)y - f/c = x
            inv[3] = 1.0f / src[3]
            inv[6] = -src[6] / src[3]
        }

        // The interesting part is inverting the nonlinear section:
        //         y                = (ax + b)^g + e.
        //         y - e            = (ax + b)^g
        //        (y - e)^1/g       =  ax + b
        //        (y - e)^1/g - b   =  ax
        //   (1/a)(y - e)^1/g - b/a =   x
        //
        // To make that fit our form, we need to move the (1/a) term inside the exponentiation:
        //   let k = (1/a)^g
        //   (1/a)( y -  e)^1/g - b/a = x
        //        (ky - ke)^1/g - b/a = x
        val k = src[1].toDouble().pow((-src[0]).toDouble()).toFloat()  // (1/a)^g == a^-g
        inv[0] = 1.0f / src[0]
        inv[1] = k
        inv[2] = -k * src[5]
        inv[5] = -src[2] / src[1]

        // We need to enforce the same constraints here that we do when fitting a curve,
        // a >= 0 and ad+b >= 0.  These constraints are checked by classify(), so they're true
        // of the source function if we're here.

        // Just like when fitting the curve, there's really no way to rescue a < 0.
        if (inv[1] < 0) {
            throw IllegalArgumentException()
        }

        // On the other hand we can rescue an ad+b that's gone slightly negative here.
        if (inv[1] * inv[4] + inv[2] < 0) {
            inv[2] = -inv[1] * inv[4]
        }

        assert(inv[1] >= 0)
        assert(inv[1] * inv[4] + inv[2] >= 0)

        // Now in principle we're done.
        // But to preserve the valuable invariant inv(src(1.0f)) == 1.0f, we'll tweak
        // e or f of the inverse, depending on which segment contains src(1.0f).
        var s = skcmsTransferfunctionEval(src, 1.0f)

        val sign = if (s < 0) -1.0f else 1.0f
        s *= sign

        if (s < inv[4]) {
            inv[6] = 1.0f - sign * inv[3] * s
        } else {
            inv[5] =
                (1.0f - sign * (inv[1] * s + inv[2]).toDouble().pow(inv[0].toDouble())).toFloat()
        }

        System.arraycopy(inv, 0, dest, 0, inv.size)
    }

    fun onContextCreated(destColorSpace: ColorSpace) {
        program = createProgram(ultraHDRVertexShader, ultraHDRFragmentShader)
        if (program == 0) return

        maPositionHandle = glGetAttribLocation(program, "aPosition")
        checkGlError("glGetAttribLocation aPosition")

        if (maPositionHandle == -1)
            throw RuntimeException("Could not get attrib location for aPosition")

        maTextureHandle = glGetAttribLocation(program, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")

        if (maTextureHandle == -1)
            throw java.lang.RuntimeException("Could not get attrib location for aTextureCoord")

        muMVPMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")

        if (muMVPMatrixHandle == -1)
            throw java.lang.RuntimeException("Could not get attrib location for uMVPMatrix")

        destTFHandle = glGetUniformLocation(program, "destTF")
        wHandle = uniform("W")

        val textures = IntArray(2)
        glGenTextures(2, textures, 0)

        if (bitmap.config == Bitmap.Config.HARDWARE ||
            bitmap.gainmap!!.gainmapContents.config == Bitmap.Config.HARDWARE
        )
            throw IllegalArgumentException("Cannot handle HARDWARE bitmaps")

        textureID = textures[0]
        gainmapTextureID = textures[1]
        glBindTexture(GL_TEXTURE_2D, textureID)
        setupTexture(bitmap)
        glBindTexture(GL_TEXTURE_2D, gainmapTextureID)
        setupTexture(bitmap.gainmap!!.gainmapContents)

        // Bind the base & gainmap textures
        glUseProgram(program)
        val textureLoc = glGetUniformLocation(program, "base")
        glUniform1i(textureLoc, 0)
        val gainmapLoc = glGetUniformLocation(program, "gainmap")
        glUniform1i(gainmapLoc, 1)

        // Bind the base transfer function
        val srcTF = FloatArray(7)
        System.arraycopy(kSRGB, 0, srcTF, 0, srcTF.size)

        val srcColorspace: ColorSpace.Rgb = bitmap.colorSpace!! as ColorSpace.Rgb
        srcColorspace.transferParameters!!.let { params ->
            srcTF[0] = params.g.toFloat()
            srcTF[1] = params.a.toFloat()
            srcTF[2] = params.b.toFloat()
            srcTF[3] = params.c.toFloat()
            srcTF[4] = params.d.toFloat()
            srcTF[5] = params.e.toFloat()
            srcTF[6] = params.f.toFloat()
        }

        val srcTfHandle = glGetUniformLocation(program, "srcTF")
        glUniform1fv(srcTfHandle, 7, srcTF, 0)

        val srcD50 = ColorSpace.adapt(srcColorspace, ColorSpace.ILLUMINANT_D50) as ColorSpace.Rgb
        val destD50 = ColorSpace.adapt(destColorSpace, ColorSpace.ILLUMINANT_D50) as ColorSpace.Rgb

        val gamutTransform = mul3x3(destD50.inverseTransform, srcD50.transform)
        val gamutHandle = glGetUniformLocation(program, "gamutTransform")
        glUniformMatrix3fv(gamutHandle, 1, false, gamutTransform, 0)

        val gainmap = bitmap.gainmap!!
        val isAlpha = gainmap.gainmapContents.config == Bitmap.Config.ALPHA_8
        val gainmapGamma = gainmap.gamma
        val noGamma = gainmapGamma[0] == 1f && gainmapGamma[1] == 1f && gainmapGamma[2] == 1f

        glUniform1i(uniform("gainmapIsAlpha"), if (isAlpha) 1 else 0)
        glUniform1i(uniform("noGamma"), if (noGamma) 1 else 0)
        setVec3Uniform("gainmapGamma", gainmapGamma)
        setLogVec3Uniform("logRatioMin", gainmap.ratioMin)
        setLogVec3Uniform("logRatioMax", gainmap.ratioMax)
        setVec3Uniform("epsilonSdr", gainmap.epsilonSdr)
        setVec3Uniform("epsilonHdr", gainmap.epsilonHdr)

        displayRatioSdr = gainmap.minDisplayRatioForHdrTransition
        displayRatioHdr = gainmap.displayRatioForFullHdr

        Matrix.setLookAtM(
            vMatrix,
            0,
            0f, 0f, -5f,
            0f, 0f, 0f,
            0f, 1.0f, 0.0f,
        )
    }

    fun onContextDestroyed() {
    }

    private fun uniform(name: String) = glGetUniformLocation(program, name)

    private fun setVec3Uniform(name: String, vec3: FloatArray) =
        glUniform3f(uniform(name), vec3[0], vec3[1], vec3[2])

    private fun setLogVec3Uniform(name: String, vec3: FloatArray) {
        val log = floatArrayOf(
            ln(vec3[0].toDouble()).toFloat(),
            ln(vec3[1].toDouble()).toFloat(),
            ln(vec3[2].toDouble()).toFloat(),
        )
        setVec3Uniform(name, log)
    }

    private fun setupTexture(bitmap: Bitmap?) {
        glTexParameterf(
            GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
            GL_LINEAR.toFloat(),
        )

        glTexParameterf(
            GL_TEXTURE_2D,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR.toFloat(),
        )

        glTexParameteri(
            GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
            GL_REPEAT,
        )

        glTexParameteri(
            GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
            GL_REPEAT,
        )

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
    }

    fun onDrawFrame(
        width: Int,
        height: Int,
        hdrSdrRatio: Float,
        bufferWidth: Int,
        bufferHeight: Int,
        transform: FloatArray,
    ) {
        glViewport(0, 0, bufferWidth, bufferHeight)

        Matrix.orthoM(
            orthoMatrix, 0, 0f, bufferWidth.toFloat(), 0f,
            bufferHeight.toFloat(), -1f, 1f,
        )

        Matrix.multiplyMM(projMatrix, 0, orthoMatrix, 0, transform, 0)

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)
        glUseProgram(program)
        checkGlError("glUseProgram")

        // This isn't a good assumption to make, and applying a color gamut matrix from the source
        // to the destination should be added.
        trfnApplyGain(kSRGB, hdrSdrRatio, destTF)
        invertTrfn(destTF, destTF)
        glUniform1fv(destTFHandle, 7, destTF, 0)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureID)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, gainmapTextureID)

        val targetRatio = ln(hdrSdrRatio.toDouble()) - ln(displayRatioSdr.toDouble())
        val maxRatio = ln(displayRatioHdr.toDouble()) - ln(displayRatioSdr.toDouble())
        val wUnclamped = targetRatio / maxRatio
        val w = max(min(wUnclamped, 1.0), 0.0).toFloat()
        glUniform1f(wHandle, w)

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        glVertexAttribPointer(
            maPositionHandle, 3, GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices,
        )
        checkGlError("glVertexAttribPointer maPosition")
        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")
        glVertexAttribPointer(
            maTextureHandle, 2, GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices,
        )
        checkGlError("glVertexAttribPointer maTextureHandle")
        glEnableVertexAttribArray(maTextureHandle)
        checkGlError("glEnableVertexAttribArray maTextureHandle")

        Matrix.setRotateM(mMatrix, 0, 0f, 0f, 0f, 1.0f)
        Matrix.scaleM(mvpMatrix, 0, mMatrix, 0, width.toFloat(), height.toFloat(), 1f)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0)

        glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvpMatrix, 0)
        glDrawArrays(GL_TRIANGLES, 0, 6)
        checkGlError("glDrawArrays")
    }

    companion object {
        /**
         * Location of UltraHDR vertex shader program.
         */
        private const val FILE_ULTRAHDR_VERTEX_SHADER = "shaders/vs_uhdr_texture_sampling.vert"

        /**
         * Location of UltraHDR fragment shader program.
         */
        private const val FILE_ULTRAHDR_FRAGMENT_SHADER = "shaders/fs_uhdr_tonemapper.frag"

        private const val FLOAT_SIZE_BYTES = 4
        private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

        private val triangleVerticesData = floatArrayOf(
            // X, Y, Z, U, V
            0f, 0f, .5f, 0f, 0f,
            0f, 1f, .5f, 0f, 1f,
            1f, 0f, .5f, 1f, 0f,
            1f, 1f, .5f, 1f, 1f,
            0f, 1f, .5f, 0f, 1f,
            1f, 0f, .5f, 1f, 0f,
        )
    }
}