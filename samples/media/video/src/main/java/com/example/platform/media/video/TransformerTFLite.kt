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

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.platform.media.video.databinding.TransformerTfliteLayoutBinding
import com.google.android.catalog.framework.annotations.Sample
import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@UnstableApi
@Sample(
    name = "Transformer and TFLite",
    description = "This sample demonstrates using Transformer with TFLite by applying a selected art style to a video.",
    documentation = "https://developer.android.com/guide/topics/media/transformer",
    tags = ["Transformer"],
)
class TransformerTFLite : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: TransformerTfliteLayoutBinding? = null
    private val binding get() = _binding!!

    /**
     * cache file used to save the output result of the transcoding operation.
     */
    private var externalCacheFile: File? = null

    /**
     * [ExoPlayer], used to playback the output of the transcoding operation.
     */
    private var player: ExoPlayer? = null

    /**
     * [Stopwatch], used to track the progress of the transcoding operation.
     */
    private var exportStopwatch: Stopwatch? = null

    /**
     * [Transformer.Listener] receives callbacks for export events.
     */
    private val transformerListener: Transformer.Listener =
        object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                Log.i(TAG, "Transformation is completed")
                exportStopwatch?.stop()
                playOutput()
                // Don't re-enable options to export with another style, because the result looks
                // glitchy on subsequent exports. Consider looking into why this is the case.
            }

            override fun onError(
                composition: Composition, result: ExportResult,
                exception: ExportException,
            ) {
                exportStopwatch?.stop()
                Log.i(TAG, "Error during transformation:" + exception.errorCodeName)
            }
        }

    /**
     * Plays export output in [ExoPlayer].
     */
    private fun playOutput() {
        Log.i(TAG, "Initiate playback using ExoPlayer.")
        lifecycleScope.launch { playbackUsingExoPlayer() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = TransformerTfliteLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.exportButton.setOnClickListener {
            binding.exportButton.isEnabled = false
            for (i in 0..<binding.styleRadioGroup.childCount) {
                binding.styleRadioGroup.getChildAt(i).isEnabled = false
            }
            exportComposition()
        }

        exportStopwatch = Stopwatch.createUnstarted(
            object : Ticker() {
                override fun read(): Long {
                    return SystemClock.elapsedRealtimeNanos()
                }
            },
        )
        try {
            externalCacheFile = createExternalCacheFile("transformer-output.mp4")
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    /**
     * Creates an external cache file that will be used to save the [Composition] output.
     */
    @Throws(IOException::class)
    private fun createExternalCacheFile(fileName: String): File {
        val file = File(requireActivity().externalCacheDir, fileName)
        check(!(file.exists() && !file.delete())) { "Could not delete the previous export output file" }
        check(file.createNewFile()) { "Could not create the export output file" }
        return file
    }

    /**
     * Sets up [Transformer] and [Composition] and starts the transcoding operation.
     * [Transformer] internal processing is done on separate thread.
     */
    private fun exportComposition() {
        val originalVideo = EditedMediaItem.Builder(
            // apply effects only on the first item
            MediaItem.fromUri(URI_VIDEO1),
        )
            .setEffects(getSelectedStyle())
            .build()
        // set up a Transformer instance and add a callback listener.
        val transformer = Transformer.Builder(requireContext())
            .addListener(transformerListener)
            .build()
        val filePath: String = externalCacheFile!!.absolutePath
        transformer.start(originalVideo, filePath)
        startTimer(transformer)
    }

    private fun getSelectedStyle(): Effects {
        val selectedEffects = mutableListOf<Effect>()

        val selectedStyle = binding.styleRadioGroup.checkedRadioButtonId
        var selectedStyleAsset = ""
        when (selectedStyle) {
            R.id.style1 -> selectedStyleAsset = "style1.jpg"
            R.id.style2 -> selectedStyleAsset = "style2.jpg"
            R.id.style3 -> selectedStyleAsset = "style3.jpg"
            else -> {
                Log.e(TAG, "No style selected")
            }
        }

        selectedEffects.add(
            GlEffect { context, _ ->
                StyleTransferShaderProgram(
                    context,
                    selectedStyleAsset,
                )
            },
        )

        return Effects(
            /* audioProcessors= */ listOf(),
            /* videoEffects= */ selectedEffects,
        )
    }

    /**
     * Sets up an [ExoPlayer] instance to playback the output cache file.
     */
    private suspend fun playbackUsingExoPlayer() = withContext(Dispatchers.Main) {
        binding.mediaPlayer.useController = true

        val player = ExoPlayer.Builder(requireContext()).build()
        player.setMediaItem(MediaItem.fromUri("file://" + externalCacheFile!!.absolutePath))
        player.prepare()

        // Attaching player to player view
        binding.mediaPlayer.player = player

        // Play back video
        player.play()
    }

    /**
     * Releases an [ExoPlayer] instance and resets the [Stopwatch].
     */
    private fun releasePlayer() {
        exportStopwatch!!.reset()
        binding.mediaPlayer.player?.stop()
        player?.release()
        player = null
    }

    /**
     * Sets up a timer and [Handler] to handle progress updates from the transcoding operation.
     */
    private fun startTimer(transformer: Transformer) {
        exportStopwatch?.reset()
        exportStopwatch?.start()
        val mainHandler = Handler(Looper.getMainLooper())
        val progressHolder = ProgressHolder()
        mainHandler.post(
            object : Runnable {
                override fun run() {
                    if (transformer.getProgress(progressHolder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                        binding.exportProgressText.text = getString(
                            R.string.export_timer,
                            exportStopwatch!!.elapsed(
                                TimeUnit.SECONDS,
                            ),
                        )
                        mainHandler.postDelayed(this, 1000)
                    }
                }
            },
        )
    }

    companion object {
        /**
         * Class Tag
         */
        private val TAG = TransformerTFLite::class.java.simpleName

        /**
         * Video and audio assets
         */
        private const val URI_VIDEO1 =
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4"
    }
}
