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

package com.example.platform.ui.text

import android.app.Application
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.example.platform.ui.text.databinding.DownloadableFontsFragmentBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Downloadable Fonts",
    description = "Download fonts instead of bundling them in the app resources.",
    documentation = "https://developer.android.com/develop/ui/views/text-and-emoji/downloadable-fonts",
    tags = ["text"],
)
class DownloadableFontsFragment : Fragment(R.layout.downloadable_fonts_fragment) {

    private val viewModel: DownloadableFontsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = DownloadableFontsFragmentBinding.bind(view)
        viewModel.typeface.observe(viewLifecycleOwner) { typeface ->
            // The downloadable font can be set to TextViews as `Typeface`.
            binding.sampleText.typeface = typeface
        }
        viewModel.fontFamilyIsValid.observe(viewLifecycleOwner) { isValid ->
            if (isValid) {
                binding.fontFamilyLayout.isErrorEnabled = false
            } else {
                binding.fontFamilyLayout.isErrorEnabled = true
                binding.fontFamilyLayout.error = getString(R.string.invalid_font_family)
            }
        }
        viewModel.applyIsEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.apply.isEnabled = isEnabled
        }
        viewModel.fontRequestErrorReason.observe(viewLifecycleOwner) { errorReason ->
            if (errorReason == 0) {
                binding.fontRequestError.visibility = View.GONE
            } else {
                binding.fontRequestError.visibility = View.VISIBLE
                binding.fontRequestError.text = getString(R.string.font_request_error, errorReason)
            }
        }
        binding.fontFamily.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                viewModel.fontFamilies,
            ),
        )
        binding.fontFamily.doOnTextChanged { s, _, _, _ -> viewModel.updateFontFamily(s.toString()) }
        binding.apply.setOnClickListener { viewModel.applyFont() }
    }
}

private const val PROVIDER_AUTHORITY = "com.google.android.gms.fonts"
private const val PROVIDER_PACKAGE = "com.google.android.gms"

class DownloadableFontsViewModel(application: Application) : AndroidViewModel(application) {

    /** The list of available font families. */
    val fontFamilies: Array<String> = application.resources.getStringArray(R.array.font_families)

    /** Fonts are downloaded in this thread. */
    private val handlerThread = HandlerThread("FontRequest").apply { start() }
    private val handler = Handler(handlerThread.looper)

    /** The current font family shown in the UI. */
    private val _fontFamily = MutableLiveData("")

    /** Whether the current font family is valid. */
    private val _fontFamilyValid = MutableLiveData(false)

    /** Whether the UI had a valid font family at least once. No error is shown until then. */
    private val _validOnce = MutableLiveData(false)

    /** The error message to be shown on the font family. */
    val fontFamilyIsValid = _validOnce.switchMap { validOnce ->
        _fontFamilyValid.map { valid ->
            valid || !validOnce
        }
    }

    /** Non-zero error code if the font request has failed. */
    val fontRequestErrorReason = MutableLiveData(0)

    /** Whether the "Apply" button should be enabled. */
    val applyIsEnabled: LiveData<Boolean> = _fontFamilyValid

    /** The latest typeface applied to the sample text. */
    val typeface = MutableLiveData(Typeface.DEFAULT)

    override fun onCleared() {
        handlerThread.quitSafely()
    }

    /** Called when the user types in a font family. */
    fun updateFontFamily(fontFamily: String) {
        _fontFamily.value = fontFamily
        val valid = fontFamily in fontFamilies
        _fontFamilyValid.value = valid
        if (valid) _validOnce.value = true
    }

    /** Called when the "Apply" button is clicked. This is where we download the specified font. */
    fun applyFont() {
        val request = FontRequest(
            PROVIDER_AUTHORITY,
            PROVIDER_PACKAGE,
            // Query string to specify the font to download.
            "name=${_fontFamily.value}&weight=400&width=10&italic=0.0&besteffort=true",
            // The certificate.
            R.array.com_google_android_gms_fonts_certs,
        )
        val callback = object : FontsContractCompat.FontRequestCallback() {
            override fun onTypefaceRetrieved(t: Typeface?) {
                if (t != null) {
                    typeface.value = t
                }
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                fontRequestErrorReason.value = reason
            }
        }
        FontsContractCompat.requestFont(getApplication(), request, callback, handler)
    }
}
