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
    name = "Single Permission",
    description = "Shows the recommended flow to request single runtime permissions",
    documentation = "https://developer.android.com/training/permissions/requesting",
    tags = ["permissions"]
)
class SinglePermission : Fragment() {

    companion object {
        private const val CALL_PHONE = Manifest.permission.CALL_PHONE
    }

    // Register the contract in your fragment/activity and update state based on the result
    private val callPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionScreenState.value = getState()

            // Gracefully reflect the decision on the UI, for simplicity for the sample we just show
            // a Toast
            if (!granted) {
                Toast.makeText(
                    requireContext(), "Cannot call without that permission", Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Compose specific state mechanism, you would implement something similar for your Views with
    // a class to manage the state.
    private val permissionScreenState: MutableState<PermissionScreenState> by lazy {
        mutableStateOf(getState())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // Compose specific: every time the state changes it updates the UI
                PermissionScreen(
                    state = permissionScreenState.value,
                    onClick = {
                        handleClick()
                    },
                    onRationaleReply = { accepted ->
                        if (accepted) {
                            callPermissionRequest.launch(CALL_PHONE)
                        }
                        // Reset the state
                        permissionScreenState.value = getState()
                    })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionScreenState.value = getState()
    }

    /**
     * This method contains the logic when certain action requires a permissions by:
     *   - If granted -> do action
     *   - Otherwise, if we should show a rationale -> display before requesting the permission
     *   - Or directly request it
     */
    private fun handleClick() {
        when {
            isPermissionGranted() -> {
                Toast.makeText(
                    requireContext(), "Faking a call...", Toast.LENGTH_SHORT
                ).show()
            }

            shouldShowRequestPermissionRationale(CALL_PHONE) -> {
                permissionScreenState.value = PermissionScreenState(
                    title = "Call a phone",
                    buttonText = "Grant permission",
                    rationale = "In order to perform the call you need to grant access by accepting the next permission dialog.\n\nWould you like to continue?"
                )
            }

            else -> {
                callPermissionRequest.launch(CALL_PHONE)
            }
        }
    }

    /**
     * Define your UI state based on the permission status.
     */
    private fun getState() = if (isPermissionGranted()) {
        PermissionScreenState(
            title = "You can now call!", buttonText = "Call"
        )
    } else {
        PermissionScreenState(
            title = "Call a phone", buttonText = "Grant permission"
        )
    }

    private fun isPermissionGranted() = ContextCompat.checkSelfPermission(
        requireContext(), CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED
}
