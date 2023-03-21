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

package com.example.platform.privacy.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Package Visibility",
    description = "A sample that showcase how the package visibility queries affects the available packages",
    documentation = "https://developer.android.com/training/package-visibility"
)
@Composable
fun PackageVisibility() {
    val context = LocalContext.current

    // Get all installed apps. From Android 11 onwards the system will filter them based on the
    // visible packages for your app. You can control the visible apps by Modify the queries
    // elements in the AndroidManifest.xml to get different package visibilities.
    val installedApps by produceState(initialValue = InstalledApps()) {
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(
                    PackageManager.GET_META_DATA.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }
        value = InstalledApps(
            infoList = list,
            userApps = list.filterNot { it.isSystemApp() }.map { it.getLabel(context) }.sorted(),
            systemApps = list.filter { it.isSystemApp() }.map { it.getLabel(context) }.sorted()
        )
    }

    // For example if we would like to check if there is a browser before launching an intent
    // we would need to request the visibility in the AndroidManifest before we can query the intent
    val availableBrowsers by produceState(initialValue = emptyList()) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://developer.android.com/training/package-visibility")
        )
        value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.map { it.loadLabel(context.packageManager).ifBlank { it.resolvePackageName }.toString() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "The visible packages depends on the OS version and the queries defined in the AndroidManifest",
                style = MaterialTheme.typography.labelMedium
            )
        }
        item {
            Text(text = "Stats", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            val count = installedApps.infoList.count { it.isSystemApp() }
            Text(text = "System packages: $count")
        }
        item {
            Text(text = "Available browsers: ${availableBrowsers.size}")
        }
        item {
            Text(text = "Total packages: ${installedApps.infoList.size}")
        }
        item {
            Text(
                text = "User apps",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        items(installedApps.userApps) {
            Text(text = it)
        }
        item {
            Text(
                text = "Available browsers",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        items(availableBrowsers) {
            Text(text = it)
        }
        item {
            Text(
                text = "System apps",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        items(installedApps.systemApps) {
            Text(text = it)
        }
    }
}

private fun ApplicationInfo.getLabel(context: Context) =
    loadLabel(context.packageManager).ifBlank { packageName }.toString()

private data class InstalledApps(
    val infoList: List<ApplicationInfo> = emptyList(),
    val userApps: List<String> = emptyList(),
    val systemApps: List<String> = emptyList(),
)

private fun ApplicationInfo.isSystemApp() =
    (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
