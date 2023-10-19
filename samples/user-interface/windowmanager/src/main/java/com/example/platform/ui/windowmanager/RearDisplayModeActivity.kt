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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.core.ExperimentalWindowApi
import com.example.platform.ui.windowmanager.databinding.ActivityRearDisplayModeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@OptIn(ExperimentalWindowApi::class)
class RearDisplayModeActivity : AppCompatActivity(), WindowAreaSessionCallback {

    private lateinit var binding: ActivityRearDisplayModeBinding

    private lateinit var windowAreaController: WindowAreaController
    private lateinit var displayExecutor: Executor
    private var rearDisplaySession: WindowAreaSession? = null
    private var rearDisplayWindowAreaInfo: WindowAreaInfo? = null
    private var rearDisplayStatus: WindowAreaCapability.Status =
        WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED

    private val rearDisplayOperation = WindowAreaCapability.Operation.OPERATION_TRANSFER_ACTIVITY_TO_AREA

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityRearDisplayModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayExecutor = ContextCompat.getMainExecutor(this)
        windowAreaController = WindowAreaController.getOrCreate()

        binding.button.setOnClickListener {
            toggleRearDisplayMode()
        }

        updateCapabilities()
    }

    private fun toggleRearDisplayMode() {
        if(rearDisplayStatus == WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE) {
            if(rearDisplaySession == null) {
                rearDisplaySession = rearDisplayWindowAreaInfo?.getActiveSession(
                    rearDisplayOperation
                )
            }
            rearDisplaySession?.close()
        } else {
            rearDisplayWindowAreaInfo?.token?.let { token ->
                windowAreaController.transferActivityToWindowArea(
                    token = token,
                    activity = this,
                    executor = displayExecutor,
                    windowAreaSessionCallback = this
                )
            }
        }
    }

    private fun updateCapabilities() {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowAreaController.windowAreaInfos
                    .map { info -> info.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING } }
                    .onEach { info -> rearDisplayWindowAreaInfo = info }
                    .map { it?.getCapability(rearDisplayOperation)?.status ?: WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED }
                    .distinctUntilChanged()
                    .collect {
                        rearDisplayStatus = it
                        updateUI()
                    }
            }
        }
    }

    private fun updateUI() {
        if(rearDisplaySession != null) {
            binding.button.isEnabled = true
            binding.status.text = "Disable RearDisplay Mode"
        } else {
            when(rearDisplayStatus) {
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED -> {
                    binding.button.isEnabled = false
                    binding.status.text = "RearDisplay is not supported on this device"
                }
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNAVAILABLE -> {
                    binding.button.isEnabled = false
                    binding.status.text = "RearDisplay is not currently available"
                }
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE -> {
                    binding.button.isEnabled = true
                    binding.status.text = "Enable RearDisplay Mode"
                }
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE -> {
                    binding.button.isEnabled = true
                    binding.status.text = "Disable RearDisplay Mode"
                }
                else -> {
                    binding.button.isEnabled = false
                    binding.status.text = "RearDisplay status is unknown"
                }
            }
        }
    }

    override fun onSessionEnded(t: Throwable?) {
        if(t != null) {
            println("Something was broken: ${t.message}")
        }
    }

    override fun onSessionStarted(session: WindowAreaSession) {
    }
}