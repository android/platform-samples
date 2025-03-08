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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/**
 * Represents an API surface or sample.
 */
interface CatalogItem {
    val id: String
    val name: String
    val description: String?
}

/**
 * Screen rendering list of items with their descriptions (API surfaces or samples)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    item: CatalogItem,
    subItems: Map<String, CatalogItem>,
    onNavigateToSubItem: (CatalogItem) -> Unit = {},
) {
    val list = remember { subItems.values.toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name) },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(8.dp),
        ) {
            item {
                Text(
                    text = "Samples",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(list) { subItem ->
                CatalogScreenItem(item = subItem, onClick = onNavigateToSubItem)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CatalogScreenItem(
    item: CatalogItem,
    onClick: (CatalogItem) -> Unit,
) {
    OutlinedCard(onClick = { onClick(item) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = item.name, style = MaterialTheme.typography.titleSmall)
                if (item.description != null) {
                    Text(
                        text = item.description!!,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}