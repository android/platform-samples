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

package com.example.platform.ui.haptics

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.platform.ui.haptics.basic.HapticsBasicRoute
import com.example.platform.ui.haptics.basic.HapticsBasicViewModel
import com.example.platform.ui.haptics.bounce.BounceRoute
import com.example.platform.ui.haptics.bounce.BounceViewModel
import com.example.platform.ui.haptics.expand.ExpandRoute
import com.example.platform.ui.haptics.expand.ExpandViewModel
import com.example.platform.ui.haptics.resist.ResistRoute
import com.example.platform.ui.haptics.resist.ResistViewModel
import com.example.platform.ui.haptics.wobble.WobbleRoute
import com.example.platform.ui.haptics.wobble.WobbleViewModel
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.launch

@Sample(
    name = "Haptics - 1. Vibration effects",
    description = "Shows various vibration effects.",
    documentation = "https://source.android.com/docs/core/interaction/haptics",
    tags = ["haptics"],
)
@Composable
fun HapticsBasic() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: HapticsBasicViewModel = viewModel(
        factory = HapticsBasicViewModel.provideFactory(application),
    )
    val coroutineScope = rememberCoroutineScope()
    Box {
        HapticsBasicRoute(
            viewModel = viewModel,
            onShowMessage = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Sample(
    name = "Haptics - 2. Resist",
    description = "Simulates resistance by increasing the intensity and reducing the duration between vibration effects.",
    documentation = "https://source.android.com/docs/core/interaction/haptics",
    tags = ["haptics"],
)
@Composable
fun Resist() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: ResistViewModel = viewModel(
        factory = ResistViewModel.provideFactory(application),
    )
    ResistRoute(viewModel = viewModel)
}

@Sample(
    name = "Haptics - 3. Expand",
    description = "Expands and collapses a circle with haptics with an added tick to sharpen that the animation has ended.",
    documentation = "https://source.android.com/docs/core/interaction/haptics",
    tags = ["haptics"],
)
@Composable
fun Expand() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: ExpandViewModel = viewModel(
        factory = ExpandViewModel.provideFactory(application),
    )
    ExpandRoute(viewModel)
}

@Sample(
    name = "Haptics - 4. Bounce",
    description = "Play primitive effects to simulate physical interactions.",
    documentation = "https://source.android.com/docs/core/interaction/haptics",
    tags = ["haptics"],
)
@Composable
fun Bounce() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: BounceViewModel = viewModel(
        factory = BounceViewModel.provideFactory(application),
    )
    BounceRoute(viewModel)
}

@Sample(
    name = "Haptics - 5. Wobble",
    description = "Play primitive effects to simulate physical interactions.",
    documentation = "https://source.android.com/docs/core/interaction/haptics",
    tags = ["haptics"],
)
@Composable
fun Wobble() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: WobbleViewModel = viewModel(
        factory = WobbleViewModel.provideFactory(application),
    )
    WobbleRoute(viewModel)
}
