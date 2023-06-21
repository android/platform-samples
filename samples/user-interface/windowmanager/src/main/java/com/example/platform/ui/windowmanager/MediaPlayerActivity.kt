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

package com.example.platform.ui.windowmanager

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.example.platform.ui.windowmanager.databinding.ActivityMediaPlayerBinding
import com.example.platform.ui.windowmanager.util.foldPosition
import com.example.platform.ui.windowmanager.util.isTableTopMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@UnstableApi class MediaPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentMediaIndex = 0
    private var playbackPosition: Long = 0

    private lateinit var binding: ActivityMediaPlayerBinding

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MediaPlayerActivity)
                    .windowLayoutInfo(this@MediaPlayerActivity)
                    .collect { newLayoutInfo ->
                        onLayoutInfoChanged(newLayoutInfo)
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        if (player == null) {
            val trackSelector = DefaultTrackSelector(this)
            trackSelector.setParameters(
                trackSelector.buildUponParameters().setMaxVideoSizeSd()
            )
            player = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()
        }
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp4))
        player?.setMediaItem(mediaItem)
        player?.playWhenReady = playWhenReady
        player?.seekTo(currentMediaIndex, playbackPosition)
        player?.prepare()
        binding.controlView.player = player
    }

    private fun releasePlayer() {
        player?.let { player ->
            playbackPosition = player.currentPosition
            currentMediaIndex = player.currentMediaItemIndex
            playWhenReady = player.playWhenReady
            player.release()
        }
        player = null
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.hide(WindowInsets.Type.systemBars())
            }
        } else {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }

    private fun centerPlayer() {
        binding.playerView.useController = true // use embedded controls
        ConstraintLayout.getSharedValues().fireNewValue(R.id.fold, 0)
    }

    private fun foldPlayer(fold: Int) {
        binding.playerView.useController = false // use custom controls
        ConstraintLayout.getSharedValues().fireNewValue(R.id.fold, fold)
        binding.controlView.show()
    }

    private fun onLayoutInfoChanged(newLayoutInfo: WindowLayoutInfo) {
        if (newLayoutInfo.displayFeatures.isEmpty()) {
            centerPlayer()
        } else {
            for (displayFeature in newLayoutInfo.displayFeatures) {
                val foldFeature = displayFeature as? FoldingFeature
                if (foldFeature != null) {
                    if (isTableTopMode(foldFeature)) {
                        // Window is full-screen and device's posture is tabletop
                        val fold = foldPosition(binding.root, foldFeature)
                        foldPlayer(fold)
                    } else {
                        centerPlayer()
                    }
                }
            }
        }
    }
}
