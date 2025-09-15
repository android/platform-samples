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

package com.example.platform.ui.share.sender

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.chooser.ChooserResult
import android.service.chooser.ChooserResult.CHOOSER_RESULT_COPY
import android.service.chooser.ChooserResult.CHOOSER_RESULT_EDIT
import android.service.chooser.ChooserResult.CHOOSER_RESULT_SELECTED_COMPONENT
import android.service.chooser.ChooserResult.CHOOSER_RESULT_UNKNOWN
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.IntentCompat


private const val TAG = "ShareResultReceiver"

@RequiresApi(22)
class ShareResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 35) {
            val chooserResult: ChooserResult? = IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_CHOOSER_RESULT,
                ChooserResult::class.java,
            )
            if (chooserResult != null) {
                Log.i(TAG, "isShortcut: ${chooserResult.isShortcut}")
                Log.i(TAG, "type: ${typeToString(chooserResult.type)}")
                Log.i(TAG, "componentName: ${chooserResult.selectedComponent}")
            } else {
                Log.i(TAG, "chooserResult is null")
            }
        } else {
            // This ComponentName represents the Activity that has received the data we shared.
            val componentName: ComponentName? = IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_CHOSEN_COMPONENT,
                ComponentName::class.java,
            )
            Log.d(TAG, "componentName: $componentName")
        }
    }

    private fun typeToString(type: Int): String {
        return when (type) {
            CHOOSER_RESULT_SELECTED_COMPONENT -> "SELECTED_COMPONENT"
            CHOOSER_RESULT_COPY -> "COPY"
            CHOOSER_RESULT_EDIT -> "EDIT"
            CHOOSER_RESULT_UNKNOWN -> "UNKNOWN"
            else -> "UNKNOWN"
        }
    }
}
