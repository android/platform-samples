/*
 *
 * Copyright (C) 2022 The Android Open Source Project
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
 *
 */

package com.google.uwb.hellouwb.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.uwb.hellouwb.data.AppSettings
import com.google.uwb.hellouwb.data.DeviceType
import com.google.uwb.hellouwb.data.ConfigType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    uiState: AppSettings,
    updateDeviceDisplayName: (String) -> Unit,
    updateDeviceType: (DeviceType) -> Unit,
    updateConfigType: (ConfigType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(
            title = { Text("Device Settings") },
            modifier = modifier
        ) },
        modifier = modifier
    ) { innerPadding ->
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
          .padding(innerPadding)
          .fillMaxWidth()
          .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        Text("Display Name")
        var fieldValue by remember { mutableStateOf(uiState.deviceDisplayName) }
        OutlinedTextField(
            fieldValue,
            onValueChange = { fieldValue = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
            KeyboardActions(
                onDone = {
                    updateDeviceDisplayName(fieldValue)
                    focusManager.clearFocus(true)
                }
            ),
            singleLine = true
        )

        Row {
            Column (horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Device Type:", Modifier.padding(20.dp))
                Row(Modifier.padding(5.dp)) {
                    val selectedValue = remember { mutableStateOf(uiState.deviceType) }
                    Column(Modifier.width(120.dp)) {
                        RadioButton(
                            selected = selectedValue.value == DeviceType.CONTROLLER,
                            onClick = {
                                updateDeviceType(DeviceType.CONTROLLER)
                                selectedValue.value = DeviceType.CONTROLLER
                            },
                        )
                        Text("Controller")
                    }
                    Column(Modifier.width(120.dp)) {
                        RadioButton(
                            selected = selectedValue.value == DeviceType.CONTROLEE,
                            onClick = {
                                updateDeviceType(DeviceType.CONTROLEE)
                                selectedValue.value = DeviceType.CONTROLEE
                            }
                        )
                        Text("Controlee")
                    }
                }
                Text("Config Type:", Modifier.padding(20.dp))
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val selectedValue = remember { mutableStateOf(uiState.configType) }
                    Row(
                        modifier = Modifier.fillMaxWidth(), // Increased width for better spacing
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedValue.value == ConfigType.CONFIG_UNICAST_DS_TWR,
                            onClick = {
                                val newType = ConfigType.CONFIG_UNICAST_DS_TWR
                                updateConfigType(newType)
                                selectedValue.value = newType
                            }
                        )
                        Text("Static Unicast")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(), // Increased width for better spacing
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedValue.value == ConfigType.CONFIG_MULTICAST_DS_TWR,
                            onClick = {
                                val newType = ConfigType.CONFIG_MULTICAST_DS_TWR
                                updateConfigType(newType)
                                selectedValue.value = newType
                            }
                        )
                        Text("Static Multicast")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(), // Increased width for better spacing
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedValue.value == ConfigType.CONFIG_PROVISIONED_UNICAST,
                            onClick = {
                                val newType = ConfigType.CONFIG_PROVISIONED_UNICAST
                                updateConfigType(newType)
                                selectedValue.value = newType
                            }
                        )
                        Text("Provisioned Unicast")
                    }
                }
            }
        }
    }
        }
}

@Preview
@Composable
fun PreviewSettingsScreen() {
    SettingsScreen(
        AppSettings.newBuilder()
            .setDeviceDisplayName("UWB")
            .setDeviceType(DeviceType.CONTROLEE)
            .setConfigType(ConfigType.CONFIG_PROVISIONED_UNICAST)
            .build(),
        {},
        {},
        {}
    )
}
