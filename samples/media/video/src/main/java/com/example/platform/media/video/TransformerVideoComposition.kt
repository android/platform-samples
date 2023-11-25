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
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.platform.media.video.databinding.TransformerCompositionLayoutBinding
import com.google.android.catalog.framework.annotations.Sample
import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@UnstableApi
@Sample(
    name = "Video Composition using Media3 Transformer",
    description = "This sample demonstrates concatenation of two video assets using Media3 " +
            "Transformer libraly.",
    documentation = "https://developer.android.com/guide/topics/media/transformer",
    tags = ["Transformer"],
)
class TransformerVideoComposition : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: TransformerCompositionLayoutBinding? = null
    private val binding get() = _binding!!

    private var externalCacheFile: File? = null
    private var player: ExoPlayer? = null
    private var exportStopwatch: Stopwatch? = null

    /**
     * [Transformer.Listener] receives callbacks for export events
     */
    private val transformerListener: Transformer.Listener =
        object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                Log.i(TAG, "Transformation is completed")
                exportStopwatch?.stop()
                playOutput()
                binding.exportButton.isEnabled = true
            }

            override fun onError(
                composition: Composition, result: ExportResult,
                exception: ExportException,
            ) {
                exportStopwatch?.stop()
                Log.i(TAG, "Error during transformation:" + exception.errorCodeName)
                binding.exportButton.isEnabled = true
            }
        }

    /**
     * Plays export output in [ExoPlayer]
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
        _binding = TransformerCompositionLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.exportButton.setOnClickListener {
            binding.exportButton.isEnabled = false
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
        // Note: there is a bug in the app setup that does not call this method
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        // Note: there is a bug in the app setup that does not call this method
        super.onStop()
        releasePlayer()
    }

    override fun onDestroyView() {
        // Note: there is a bug in the app setup that does not call this method
        super.onDestroyView()
        releasePlayer()
    }

    private fun createComposition(): Composition {
        val video1 = EditedMediaItem.Builder(
            // apply effects only on the first item
            MediaItem.fromUri(URI_ITEM1))
            .setEffects(getSelectedEffects())
            .build()
        val video2 = EditedMediaItem.Builder(
            MediaItem.fromUri(URI_ITEM2))
            .build()
        val compositionSequences = ArrayList<EditedMediaItemSequence>()
        val videoSequence = EditedMediaItemSequence(ImmutableList.of(video1, video2))
        compositionSequences.add(videoSequence)

        if (binding.backgroundAudioChip.isChecked) {
            val backgroundAudio = EditedMediaItem.Builder(MediaItem.fromUri(URI_AUDIO)).build()
            val audioSequence = EditedMediaItemSequence(
                ImmutableList.of(backgroundAudio),
                /* isLooping*/true,
            )
            compositionSequences.add(audioSequence)
        }

        return Composition.Builder(compositionSequences).build()
    }

    @Throws(IOException::class)
    private fun createExternalCacheFile(fileName: String): File {
        val file = File(requireActivity().externalCacheDir, fileName)
        check(!(file.exists() && !file.delete())) { "Could not delete the previous export output file" }
        check(file.createNewFile()) { "Could not create the export output file" }
        return file
    }

    private fun exportComposition() {
        val composition = createComposition()
        val transformer = Transformer.Builder(requireContext())
            .addListener(transformerListener)
            .build()
        val filePath: String = externalCacheFile!!.getAbsolutePath()
        transformer.start(composition, filePath)
        startTimer(transformer)
    }

    private fun getSelectedEffects(): Effects {
        val selectedEffects = ArrayList<Effect>()
        if (binding.grayscaleChip.isChecked) {
            selectedEffects.add(RgbFilter.createGrayscaleFilter())
        }
        if (binding.scaleChip.isChecked) {
            selectedEffects.add(ScaleAndRotateTransformation.Builder()
                .setScale(.2f, .2f)
                .build())
        }
        return Effects(/* audioProcessors= */ listOf(),
            /* videoEffects= */ selectedEffects)
    }

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

    private fun releasePlayer() {
        exportStopwatch!!.reset()
        binding.mediaPlayer.player?.stop()
        player?.release()
        player = null
    }

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
        private val TAG = TransformerVideoComposition::class.java.simpleName
        /**
         * Video and audio assets
         */
        private const val URI_ITEM1 =
            "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4"
        private const val URI_ITEM2 =
            "https://storage.googleapis.com/exoplayer-test-media-0/android-block-1080-hevc.mp4"
        private const val URI_AUDIO =
            "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
    }
}