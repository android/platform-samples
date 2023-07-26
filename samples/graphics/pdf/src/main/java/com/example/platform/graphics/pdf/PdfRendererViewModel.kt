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

package com.example.platform.graphics.pdf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

private const val FILENAME = "sample.pdf"

class PdfRendererViewModel(application: Application) : AndroidViewModel(application) {

    /** The file descriptor of the opened PDF file. */
    private val fileDescriptor = flow {
        val file = File(application.cacheDir, FILENAME)
        if (!file.exists()) {
            // Copy the asset file to the local cache since PdfRenderer cannot access the asset file
            // directly.
            application.assets.open(FILENAME).use { asset ->
                file.writeBytes(asset.readBytes())
            }
        }
        emit(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
    }.flowOn(Dispatchers.IO).stateInUi(null)

    /** [PdfRenderer] that renders the PDF. */
    private val pdfRenderer = fileDescriptor.map { fd ->
        if (fd == null) {
            null
        } else {
            try {
                PdfRenderer(fd)
            } catch (e: Exception) {
                Log.e("PDFRenderer", "PDF could not be rendered", e)
                null
            }
        }
    }.flowOn(Dispatchers.IO).stateInUi(null)

    /** The index of the current page. */
    private val pageIndex = MutableStateFlow(0)

    /** The number of pages in the PDF. */
    private val pageCount = pdfRenderer.map { it?.pageCount ?: 0 }.stateInUi(0)

    /** The text representation of the current page, such as "1/10", "2/10", etc. */
    val currentPage = pageIndex.combine(pageCount) { index, count ->
        "${index + 1}/$count"
    }

    /** Whether the previous page is available or not. */
    val previousEnabled = pageIndex.map { it > 0 }

    /** Whether the next page is available or not. */
    val nextEnabled = pageIndex.combine(pageCount) { index, count ->
        index + 1 < count
    }

    /** The current page rendered as a [Bitmap]. */
    val page = pdfRenderer.combine(pageIndex) { renderer, index ->
        // Use `openPage` to open a specific page in PDF.
        renderer?.openPage(index)?.use { page ->
            // Important: the destination bitmap must be ARGB (not RGB).
            Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            ).also { bitmap ->
                // Here, we render the page onto the Bitmap.
                // To render a portion of the page, use the second and third parameter. Pass nulls
                // to get the default result.
                // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last
                // parameter.
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }.flowOn(Dispatchers.IO).stateInUi(null)

    override fun onCleared() {
        try {
            pdfRenderer.value?.close()
            fileDescriptor.value?.close()
        } catch (e: Exception) {
            // silent exception when closing.
        }
    }

    fun previous() {
        pageIndex.value = (pageIndex.value - 1).coerceAtLeast(0)
    }

    fun next() {
        pageIndex.value = (pageIndex.value + 1).coerceAtMost(pageCount.value - 1)
    }

    private fun <T> Flow<T>.stateInUi(initialValue: T) = stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        initialValue
    )
}
