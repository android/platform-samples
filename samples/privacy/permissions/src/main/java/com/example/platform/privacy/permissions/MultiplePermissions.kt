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

package com.example.platform.privacy.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.platform.privacy.permissions.common.PermissionScreen
import com.example.platform.privacy.permissions.common.PermissionScreenState
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Multiple Permissions",
    description = "Shows the recommended flow to request multiple RELATED runtime permissions",
    documentation = "https://developer.android.com/training/permissions/requesting",
    tags = ["permissions"]
)
class MultiplePermissions : Fragment() {

    companion object {
        private const val CAMERA = Manifest.permission.CAMERA
        private const val MIC = Manifest.permission.RECORD_AUDIO
    }

    // Register the contract in your fragment/activity and handle the result
    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            doPermissionAction()
        }

    // Compose specific state mechanism, you would implement something similar for your Views with
    // a class to manage the state.
    private val permissionScreenState: MutableState<PermissionScreenState> by lazy {
        mutableStateOf(getState())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Using a simple composable UI that displays a title, button and/or a dialog based on the
        // provided [PermissionScreenState]
        return ComposeView(requireContext()).apply {
            setContent {
                // Compose specific: every time the state changes it updates the UI
                PermissionScreen(
                    state = permissionScreenState.value,
                    onClick = {
                        doAction()
                    },
                    onRationaleReply = { accepted ->
                        doRationaleAction(accepted)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update permission state when the activity is visible again. Something might have changed
        permissionScreenState.value = getState()
    }

    /**
     * This method contains the logic when certain action requires a permissions by:
     *   - If all granted -> do action
     *   - If minimal required are granted (i.e CAMERA) -> show rationale if required or request
     *   missing permission
     *   - Otherwise, check if we should show a rationale -> display before requesting the permission
     *   - Or directly request all of them.
     */
    private fun doAction() {
        val camera = isPermissionGranted(CAMERA)
        val mic = isPermissionGranted(MIC)
        when {
            camera and mic -> {
                recordVideo()
            }

            camera and !mic -> {
                if (shouldShowRequestPermissionRationale(MIC)) {
                    permissionScreenState.value = getState().copy(
                        rationale = "In order to record a video with audio we still need access to the microphone.\nWould you like to grant it?"
                    )
                } else {
                    permissionRequest.launch(arrayOf(MIC))
                }
            }

            shouldShowRequestPermissionRationale(CAMERA) or
                    shouldShowRequestPermissionRationale(MIC) -> {
                permissionScreenState.value = getState().copy(
                    rationale = "In order to record a video with audio you need to grant access by accepting the next permission dialog.\n\nWould you like to continue?"
                )
            }

            else -> {
                permissionRequest.launch(arrayOf(CAMERA, MIC))
            }
        }
    }

    /**
     * Handles the rationale dialog flow base on the user decision by either requesting the
     * permissions or trying to satisfy the action without the permission.
     */
    private fun doRationaleAction(accepted: Boolean) {
        // If user accepted the rational request permission/s
        if (accepted) {
            permissionRequest.launch(arrayOf(CAMERA, MIC))
        } else if (isPermissionGranted(CAMERA)) {
            // if user dismissed it but we have the minimal permission, let user
            // continue with the action with a downgraded experience.
            recordVideoWithoutAudio()
        }
        // Reset the state
        permissionScreenState.value = getState()
    }

    /**
     * This method contains the logic to handle the permission result by:
     *  - Performing the action based on the granted permissions
     *     - For example continue with the video recording but without audio
     *  - Gracefully reflect the user decision on the UI
     */
    private fun doPermissionAction() {
        var newState = getState()
        val camera = isPermissionGranted(CAMERA)
        val mic = isPermissionGranted(MIC)
        when {
            camera && mic -> recordVideo()
            camera && !mic -> recordVideoWithoutAudio()
            else -> newState = newState.copy(
                errorText = "Cannot record video without camera permission"
            )
        }

        permissionScreenState.value = newState
    }

    private fun recordVideo() {
        Toast.makeText(
            requireContext(), "Recording a video...", Toast.LENGTH_SHORT
        ).show()
    }

    private fun recordVideoWithoutAudio() {
        Toast.makeText(
            requireContext(), "Recording a video without audio...", Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Define your UI state based on the permission status.
     *
     * For example, changing the CTA label based on the missing permission/s
     */
    private fun getState(): PermissionScreenState {
        val camera = isPermissionGranted(CAMERA)
        val mic = isPermissionGranted(MIC)
        return when {
            camera and mic -> {
                PermissionScreenState(
                    title = "You can now record a video", buttonText = "Record"
                )
            }

            else -> {
                PermissionScreenState(
                    title = "Record a video", buttonText = if (!camera and !mic) {
                        "Grant permissions"
                    } else {
                        "Record"
                    },
                    errorText = if (camera and !mic) {
                        "Missing record audio permission"
                    } else {
                        ""
                    }
                )
            }
        }
    }

    private fun isPermissionGranted(name: String) = ContextCompat.checkSelfPermission(
        requireContext(), name
    ) == PackageManager.PERMISSION_GRANTED
}
