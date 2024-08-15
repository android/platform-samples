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

package com.example.android.pip

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.PictureInPictureUiState
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import androidx.activity.trackPipAnimationHintView
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.android.pip.databinding.PipActivityBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.launch

/** Intent action for stopwatch controls from Picture-in-Picture mode.  */
private const val ACTION_STOPWATCH_CONTROL = "stopwatch_control"

/** Intent extra for stopwatch controls from Picture-in-Picture mode.  */
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val CONTROL_TYPE_CLEAR = 1
private const val CONTROL_TYPE_START_OR_PAUSE = 2

private const val REQUEST_CLEAR = 3
private const val REQUEST_START_OR_PAUSE = 4

/**
 * Demonstrates usage of Picture-in-Picture mode on phones and tablets.
 */
@Sample(
    name = "Picture in Picture (PiP) - Stopwatch",
    description = "Basic usage of Picture-in-Picture mode showcasing a stopwatch",
    documentation = "https://developer.android.com/develop/ui/views/picture-in-picture",
)
@RequiresApi(26)
class PiPSampleActivity : AppCompatActivity() {

    private val viewModel: PiPViewModel by viewModels()
    private lateinit var binding: PipActivityBinding

    /**
     * A [BroadcastReceiver] for handling action items on the picture-in-picture mode.
     */
    private val broadcastReceiver = object : BroadcastReceiver() {

        // Called when an item is clicked.
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != ACTION_STOPWATCH_CONTROL) {
                return
            }
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                CONTROL_TYPE_START_OR_PAUSE -> viewModel.startOrPause()
                CONTROL_TYPE_CLEAR -> viewModel.clear()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PipActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Event handlers
        binding.clear.setOnClickListener { viewModel.clear() }
        binding.startOrPause.setOnClickListener { viewModel.startOrPause() }
        binding.pip.setOnClickListener {
            enterPictureInPictureMode(updatePictureInPictureParams(viewModel.started.value == true))
        }
        // Observe data from the viewModel.
        viewModel.time.observe(this) { time -> binding.time.text = time }
        viewModel.started.observe(this) { started ->
            binding.startOrPause.setImageResource(
                if (started) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp,
            )
            updatePictureInPictureParams(started)
        }

        // Use trackPipAnimationHint view to make a smooth enter/exit pip transition.
        // See https://android.devsite.corp.google.com/develop/ui/views/picture-in-picture#smoother-transition
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                trackPipAnimationHintView(binding.stopwatchBackground)
            }
        }

        // Handle events from the action icons on the picture-in-picture mode.
        ActivityCompat.registerReceiver(
            this,
            broadcastReceiver,
            IntentFilter(ACTION_STOPWATCH_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // This is called when the activity gets into or out of the picture-in-picture mode.
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Toggle visibility of in-app buttons. They cannot be interacted in the picture-in-picture
        // mode, and their features are provided as the action icons.
        toggleControls(if (isInPictureInPictureMode) View.GONE else View.VISIBLE)
    }

    private fun toggleControls(view: Int) {
        binding.clear.visibility = view
        binding.startOrPause.visibility = view
    }

    @RequiresApi(35)
    override fun onPictureInPictureUiStateChanged(pipState: PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)
        if (pipState.isTransitioningToPip) {
            toggleControls(View.GONE)
        }
    }

    /**
     * Updates the parameters of the picture-in-picture mode for this activity based on the current
     * [started] state of the stopwatch.
     */
    private fun updatePictureInPictureParams(started: Boolean): PictureInPictureParams {
        val params = PictureInPictureParams.Builder()
            // Set action items for the picture-in-picture mode. These are the only custom controls
            // available during the picture-in-picture mode.
            .setActions(
                listOf(
                    // "Clear" action.
                    createRemoteAction(
                        R.drawable.ic_refresh_24dp,
                        R.string.clear,
                        REQUEST_CLEAR,
                        CONTROL_TYPE_CLEAR,
                    ),
                    if (started) {
                        // "Pause" action when the stopwatch is already started.
                        createRemoteAction(
                            R.drawable.ic_pause_24dp,
                            R.string.pause,
                            REQUEST_START_OR_PAUSE,
                            CONTROL_TYPE_START_OR_PAUSE,
                        )
                    } else {
                        // "Start" action when the stopwatch is not started.
                        createRemoteAction(
                            R.drawable.ic_play_arrow_24dp,
                            R.string.start,
                            REQUEST_START_OR_PAUSE,
                            CONTROL_TYPE_START_OR_PAUSE,
                        )
                    },
                ),
            )
            // Set the aspect ratio of the picture-in-picture mode.
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Turn the screen into the picture-in-picture mode if it's hidden by the "Home" button.
            params.setAutoEnterEnabled(true)
                // Disables the seamless resize. The seamless resize works great for videos where the
                // content can be arbitrarily scaled, but you can disable this for non-video content so
                // that the picture-in-picture mode is resized with a cross fade animation.
                .setSeamlessResizeEnabled(false)
        }
        return params.build().also {
            setPictureInPictureParams(it)
        }
    }

    /**
     * Creates a [RemoteAction]. It is used as an action icon on the overlay of the
     * picture-in-picture mode.
     */
    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        @StringRes titleResId: Int,
        requestCode: Int,
        controlType: Int,
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(this, iconResId),
            getString(titleResId),
            getString(titleResId),
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(ACTION_STOPWATCH_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}
