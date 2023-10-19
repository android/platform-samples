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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.core.ExperimentalWindowApi
import com.example.platform.ui.windowmanager.databinding.ActivityDualScreenBinding
import com.example.platform.ui.windowmanager.infolog.InfoLogAdapter
import com.example.platform.ui.windowmanager.util.getCurrentTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@OptIn(ExperimentalWindowApi::class)
class DualScreenActivity : AppCompatActivity(), WindowAreaPresentationSessionCallback {

    private lateinit var binding: ActivityDualScreenBinding
    private val infoLogAdapter = InfoLogAdapter()

    private lateinit var windowAreaController: WindowAreaController
    private lateinit var displayExecutor: Executor
    private var windowAreaSession: WindowAreaSessionPresenter? = null
    private var windowAreaInfo: WindowAreaInfo? = null
    private var capabilityStatus: WindowAreaCapability.Status =
        WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED

    private val presentOperation = WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA
    private val logTag = "ConcurrentDisplays"

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityDualScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.adapter = infoLogAdapter

        displayExecutor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaController.getOrCreate()

        binding.button.setOnClickListener {
            toggleDualScreenMode()
        }

        updateCapabilities()
    }

    private fun toggleDualScreenMode() {
        //here we are not checking the status because of b/302183399
        if(windowAreaSession != null) {
            windowAreaSession?.close()
        }
        else {
            windowAreaInfo?.token?.let { token ->
                windowAreaController.presentContentOnWindowArea(
                    token = token,
                    activity = this,
                    executor = displayExecutor,
                    windowAreaPresentationSessionCallback = this
                )
            }
        }
    }

    private fun updateCapabilities() {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowAreaController.windowAreaInfos
                    .map { info -> info.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING } }
                    .onEach { info -> windowAreaInfo = info }
                    .map { it?.getCapability(presentOperation)?.status ?: WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED }
                    .distinctUntilChanged()
                    .collect {
                        capabilityStatus = it
                        updateUI()
                        infoLogAdapter.notifyDataSetChanged()
                    }
            }
        }
    }

    private fun updateUI() {
        if(windowAreaSession != null) {
            binding.button.isEnabled = true
            binding.status.text = "Disable Dual Screen Mode"
        } else {
            when(capabilityStatus) {
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED -> {
                    binding.button.isEnabled = false
                    binding.status.text = "Dual Screen is not supported on this device"
                }
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNAVAILABLE -> {
                    binding.button.isEnabled = false
                    binding.status.text = "Dual Screen is not currently available"
                }
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE -> {
                    binding.button.isEnabled = true
                    binding.status.text = "Enable Dual Screen Mode"
                }
                else -> {
                    binding.button.isEnabled = false
                    binding.status.text = "Dual Screen status is unknown"
                }
            }
        }
    }

    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
        infoLogAdapter.append(getCurrentTimeString(), "Presentation session has been started")
        windowAreaSession = session
        val view = TextView(session.context)
        view.text = "Hello world, from the other screen!"
        session.setContentView(view)
        updateUI()
    }

    override fun onSessionEnded(t: Throwable?) {
        if(t != null) {
            Log.e(logTag, "Something was broken: ${t.message}")
        }
        infoLogAdapter.append(getCurrentTimeString(), "Presentation session has been ended")
        windowAreaSession = null
    }

    override fun onContainerVisibilityChanged(isVisible: Boolean) {
        infoLogAdapter.append(getCurrentTimeString(), "Presentation content is visible: $isVisible")
    }

}