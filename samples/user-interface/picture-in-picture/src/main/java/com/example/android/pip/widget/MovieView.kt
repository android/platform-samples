/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.pip.widget

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import com.example.android.pip.R
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Provides video playback. There is nothing directly related to Picture-in-Picture here.
 *
 * This is similar to [android.widget.VideoView], but it comes with a custom control
 * (play/pause, fast forward, and fast rewind).
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class MovieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {

    companion object {

        private const val TAG = "MovieView"

        /** The amount of time we are stepping forward or backward for fast-forward and fast-rewind.  */
        private const val FAST_FORWARD_REWIND_INTERVAL = 5000 // ms

        /** The amount of time until we fade out the controls.  */
        private const val TIMEOUT_CONTROLS = 3000L // ms
    }

    /**
     * Monitors all events related to [MovieView].
     */
    abstract class MovieListener {

        /**
         * Called when the video is started or resumed.
         */
        open fun onMovieStarted() {}

        /**
         * Called when the video is paused or finished.
         */
        open fun onMovieStopped() {}

        /**
         * Called when this view should be minimized.
         */
        open fun onMovieMinimized() {}
    }

    /** Shows the video playback.  */
    private val surfaceView: SurfaceView

    // Controls
    private val toggle: ImageButton
    private val shade: View
    private val fastForward: ImageButton
    private val fastRewind: ImageButton
    private val minimize: ImageButton

    /** This plays the video. This will be null when no video is set.  */
    internal var mediaPlayer: MediaPlayer? = null

    /** The resource ID for the video to play.  */
    @RawRes
    private var videoResourceId: Int = 0

    var title: String = ""

    /** Whether we adjust our view bounds or we fill the remaining area with black bars  */
    private var adjustViewBounds: Boolean = false

    /** Handles timeout for media controls.  */
    private var timeoutHandler: TimeoutHandler? = null

    /** The listener for all the events we publish.  */
    private var movieListener: MovieListener? = null

    private var savedCurrentPosition: Int = 0

    init {
        setBackgroundColor(Color.BLACK)

        // Inflate the content
        View.inflate(context, R.layout.view_movie, this)
        surfaceView = findViewById(R.id.surface)
        shade = findViewById(R.id.shade)
        toggle = findViewById(R.id.toggle)
        fastForward = findViewById(R.id.fast_forward)
        fastRewind = findViewById(R.id.fast_rewind)
        minimize = findViewById(R.id.minimize)

        // Attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.MovieView,
            defStyleAttr, R.style.Widget_PictureInPicture_MovieView,
        )
        setVideoResourceId(a.getResourceId(R.styleable.MovieView_android_src, 0))
        setAdjustViewBounds(a.getBoolean(R.styleable.MovieView_android_adjustViewBounds, false))
        title = a.getString(R.styleable.MovieView_android_title) ?: ""
        a.recycle()

        // Bind view events
        val listener = OnClickListener { view ->
            when (view.id) {
                R.id.surface -> toggleControls()
                R.id.toggle -> toggle()
                R.id.fast_forward -> fastForward()
                R.id.fast_rewind -> fastRewind()
                R.id.minimize -> movieListener?.onMovieMinimized()
            }
            // Start or reset the timeout to hide controls
            mediaPlayer?.let { player ->
                if (timeoutHandler == null) {
                    timeoutHandler = TimeoutHandler(this@MovieView)
                }
                timeoutHandler?.let { handler ->
                    handler.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
                    if (player.isPlaying) {
                        handler.sendEmptyMessageDelayed(
                            TimeoutHandler.MESSAGE_HIDE_CONTROLS,
                            TIMEOUT_CONTROLS,
                        )
                    }
                }
            }
        }
        surfaceView.setOnClickListener(listener)
        toggle.setOnClickListener(listener)
        fastForward.setOnClickListener(listener)
        fastRewind.setOnClickListener(listener)
        minimize.setOnClickListener(listener)

        // Prepare video playback
        surfaceView.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    openVideo(holder.surface)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder, format: Int,
                    width: Int, height: Int,
                ) {
                    // Do nothing
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mediaPlayer?.let { savedCurrentPosition = it.currentPosition }
                    closeVideo()
                }
            },
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mediaPlayer?.let { player ->
            val videoWidth = player.videoWidth
            val videoHeight = player.videoHeight
            if (videoWidth != 0 && videoHeight != 0) {
                val aspectRatio = videoHeight.toFloat() / videoWidth
                val width = MeasureSpec.getSize(widthMeasureSpec)
                val widthMode = MeasureSpec.getMode(widthMeasureSpec)
                val height = MeasureSpec.getSize(heightMeasureSpec)
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)
                if (adjustViewBounds) {
                    if (widthMode == MeasureSpec.EXACTLY
                        && heightMode != MeasureSpec.EXACTLY
                    ) {
                        super.onMeasure(
                            widthMeasureSpec,
                            MeasureSpec.makeMeasureSpec(
                                (width * aspectRatio).toInt(),
                                MeasureSpec.EXACTLY,
                            ),
                        )
                    } else if (widthMode != MeasureSpec.EXACTLY
                        && heightMode == MeasureSpec.EXACTLY
                    ) {
                        super.onMeasure(
                            MeasureSpec.makeMeasureSpec(
                                (height / aspectRatio).toInt(),
                                MeasureSpec.EXACTLY,
                            ),
                            heightMeasureSpec,
                        )
                    } else {
                        super.onMeasure(
                            widthMeasureSpec,
                            MeasureSpec.makeMeasureSpec(
                                (width * aspectRatio).toInt(),
                                MeasureSpec.EXACTLY,
                            ),
                        )
                    }
                } else {
                    val viewRatio = height.toFloat() / width
                    if (aspectRatio > viewRatio) {
                        val padding = ((width - height / aspectRatio) / 2).toInt()
                        setPadding(padding, 0, padding, 0)
                    } else {
                        val padding = ((height - width * aspectRatio) / 2).toInt()
                        setPadding(0, padding, 0, padding)
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                }
                return
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDetachedFromWindow() {
        timeoutHandler?.removeMessages(TimeoutHandler.MESSAGE_HIDE_CONTROLS)
        timeoutHandler = null
        super.onDetachedFromWindow()
    }

    /**
     * The raw resource id of the video to play.

     * @return ID of the video resource.
     */
    fun getVideoResourceId(): Int = videoResourceId

    /**
     * Sets the listener to monitor movie events.

     * @param movieListener The listener to be set.
     */
    fun setMovieListener(movieListener: MovieListener?) {
        this.movieListener = movieListener
    }

    /**
     * Sets the raw resource ID of video to play.

     * @param id The raw resource ID.
     */
    private fun setVideoResourceId(@RawRes id: Int) {
        if (id == videoResourceId) {
            return
        }
        videoResourceId = id
        val surface = surfaceView.holder.surface
        if (surface != null && surface.isValid) {
            closeVideo()
            openVideo(surface)
        }
    }

    fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        if (this.adjustViewBounds == adjustViewBounds) {
            return
        }
        this.adjustViewBounds = adjustViewBounds
        if (adjustViewBounds) {
            background = null
        } else {
            setBackgroundColor(Color.BLACK)
        }
        requestLayout()
    }

    /**
     * Shows all the controls.
     */
    fun showControls() {
        TransitionManager.beginDelayedTransition(this)
        shade.visibility = View.VISIBLE
        toggle.visibility = View.VISIBLE
        fastForward.visibility = View.VISIBLE
        fastRewind.visibility = View.VISIBLE
        minimize.visibility = View.VISIBLE
    }

    /**
     * Hides all the controls.
     */
    fun hideControls() {
        TransitionManager.beginDelayedTransition(this)
        shade.visibility = View.INVISIBLE
        toggle.visibility = View.INVISIBLE
        fastForward.visibility = View.INVISIBLE
        fastRewind.visibility = View.INVISIBLE
        minimize.visibility = View.INVISIBLE
    }

    /**
     * Fast-forward the video.
     */
    private fun fastForward() {
        mediaPlayer?.let { it.seekTo(it.currentPosition + FAST_FORWARD_REWIND_INTERVAL) }
    }

    /**
     * Fast-rewind the video.
     */
    private fun fastRewind() {
        mediaPlayer?.let { it.seekTo(it.currentPosition - FAST_FORWARD_REWIND_INTERVAL) }
    }

    /**
     * Returns the current position of the video. If the the player has not been created, then
     * assumes the beginning of the video.

     * @return The current position of the video.
     */
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    fun play() {
        if (mediaPlayer == null) {
            return
        }
        mediaPlayer!!.start()
        adjustToggleState()
        keepScreenOn = true
        movieListener?.onMovieStarted()
    }

    fun pause() {
        if (mediaPlayer == null) {
            adjustToggleState()
            return
        }
        mediaPlayer!!.pause()
        adjustToggleState()
        keepScreenOn = false
        movieListener?.onMovieStopped()
    }

    internal fun openVideo(surface: Surface) {
        if (videoResourceId == 0) {
            return
        }
        mediaPlayer = MediaPlayer()
        mediaPlayer?.let { player ->
            player.setSurface(surface)
            startVideo()
        }
    }

    /**
     * Restarts playback of the video.
     */
    fun startVideo() {
        mediaPlayer?.let { player ->
            player.reset()
            try {
                resources.openRawResourceFd(videoResourceId).use { fd ->
                    player.setDataSource(fd)
                    player.setOnPreparedListener { mediaPlayer ->
                        // Adjust the aspect ratio of this view
                        requestLayout()
                        if (savedCurrentPosition > 0) {
                            mediaPlayer.seekTo(savedCurrentPosition)
                            savedCurrentPosition = 0
                        } else {
                            // Start automatically
                            play()
                        }
                    }
                    player.setOnCompletionListener {
                        adjustToggleState()
                        keepScreenOn = false
                        movieListener?.onMovieStopped()
                    }
                    player.prepare()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to open video", e)
            }
        }
    }

    internal fun closeVideo() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun toggle() {
        mediaPlayer?.let { if (it.isPlaying) pause() else play() }
    }

    private fun toggleControls() {
        if (shade.visibility == View.VISIBLE) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun adjustToggleState() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                toggle.contentDescription = resources.getString(R.string.pause)
                toggle.setImageResource(R.drawable.ic_pause_64dp)
            } else {
                toggle.contentDescription = resources.getString(R.string.play)
                toggle.setImageResource(R.drawable.ic_play_arrow_64dp)
            }
        }
    }

    private class TimeoutHandler(view: MovieView) : Handler(Looper.getMainLooper()) {

        private val movieViewRef: WeakReference<MovieView> = WeakReference(view)

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_HIDE_CONTROLS -> {
                    movieViewRef.get()?.hideControls()
                }

                else -> super.handleMessage(msg)
            }
        }

        companion object {
            const val MESSAGE_HIDE_CONTROLS = 1
        }
    }
}
