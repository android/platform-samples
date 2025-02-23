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

package com.example.platform.app

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MainScreen) {
        composable<MainScreen> {
            CatalogScreen(
                item = MainScreen,
                subItems = AREAS,
                onNavigateToSubItem = { area -> navController.navigate(area as Area) },
            )
        }
        composable<Area> { backStackEntry ->
            val area = backStackEntry.toRoute<Area>()
            val subItems = remember { SAMPLE_DEMOS.filter { it.value.area == area } }

            CatalogScreen(
                item = area,
                subItems = subItems,
                onNavigateToSubItem = { sampleDemo ->
                    if (sampleDemo is ActivitySampleDemo) {
                        context.startActivity(Intent(context, sampleDemo.content))
                    } else {
                        navController.navigate("$SAMPLE_DEMO_ROUTE/${sampleDemo.id}")
                    }
                },
            )
        }

        composable(
            "$SAMPLE_DEMO_ROUTE/{$SAMPLE_DEMO_ID}",
            listOf(navArgument(SAMPLE_DEMO_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val arguments = requireNotNull(backStackEntry.arguments)
            val sampleDemoId = requireNotNull(arguments.getString(SAMPLE_DEMO_ID))
            val sampleDemo = SAMPLE_DEMOS.getValue(sampleDemoId) as ComposableSampleDemo

            Scaffold(
                topBar = { TopAppBar(title = { Text(sampleDemo.name) }) },
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(8.dp)
                        .fillMaxSize(),
                ) {
                    sampleDemo.content()
                }
            }
        }
    }
}


@Serializable
data object MainScreen : CatalogItem {
    override val id = "main"
    override val name = "Platform Samples"
    override val description = null
}

const val SAMPLE_DEMO_ROUTE = "sampledemo"
const val SAMPLE_DEMO_ID = "id"