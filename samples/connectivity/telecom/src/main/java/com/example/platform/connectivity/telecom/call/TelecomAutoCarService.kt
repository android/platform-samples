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

package com.example.platform.connectivity.telecom.call

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import com.example.platform.connectivity.telecom.launchCall

class TelecomAutoCarService :  CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return TelecomAutoSession()
    }
}

class TelecomAutoSession : Session(){
    override fun onCreateScreen(intent: Intent): Screen {
        return TelecomAutoScreen(carContext)
    }
}

class TelecomAutoScreen(carContext: CarContext) : Screen(carContext) {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onGetTemplate(): Template {

        return ListTemplate.Builder()
            .setSingleList(
                ItemList.Builder()
                    .addItem(Row.Builder().setTitle("Bob").build()).setOnSelectedListener { startOutGoingCall("Bob") }
                    .addItem(Row.Builder().setTitle("Alice").build()).setOnSelectedListener { startOutGoingCall("Alice") }
                    .addItem(Row.Builder().setTitle("Luke").build()).setOnSelectedListener { startOutGoingCall("Luke") }
                    // Add more items as needed
                    .build()
            )
            .setTitle("Contacts")
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startOutGoingCall(name: String){
        carContext.launchCall(
            action = TelecomCallService.ACTION_OUTGOING_CALL,
            name = name,
            uri = Uri.parse("tel:54321"),
        )
    }
}
