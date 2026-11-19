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

package com.example.platform.performance.memory

import android.app.MemoryBudgetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Looper
import android.util.Log
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.platform.shared.MinSdkFullBox
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.createBitmap

private const val TAG = "MemoryBudgetSample"
private const val BYTES_PER_MB = 1024L * 1024L
private const val GALLERY_BUDGET_BYTES = 96L * BYTES_PER_MB
private const val TIGHT_PRESSURE_TEST_BUDGET_BYTES = 48L * BYTES_PER_MB
private const val MAX_PREVIEW_CACHE_BYTES = 32 * 1024 * 1024
private const val GALLERY_CACHE_BYTES = 8 * 1024 * 1024
private const val TILE_DIMENSION_PX = 1024 // 1024 * 1024 * 4 bytes (ARGB_8888) = 4 MB per tile

private enum class WorkloadMode(val label: String) {
    BASELINE("Manifest / Unconstrained"),
    GALLERY("Lightweight Gallery (96 MB)"),
    TIGHT_TEST("Tight Pressure Test (48 MB)"),
}

@RequiresApi(Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
private data class MemorySnapshot(
    val processUsageBytes: Long = 0L,
    val packageUsageBytes: Long = 0L,
    val processBudgetBytes: Long = MemoryBudgetManager.LIMIT_IS_DISABLED,
    val packageBudgetBytes: Long = MemoryBudgetManager.LIMIT_IS_DISABLED,
)

@Composable
fun MemoryBudgetSample() {
    MinSdkFullBox(minSdkFull = Build.VERSION_CODES_FULL.CINNAMON_BUN_2) {
        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) &&
            (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
        ) {
            MemoryBudgetContent()
        }
    }
}

@RequiresApi(Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
@Composable
private fun MemoryBudgetContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val budgetManager = remember(context) {
        context.getSystemService(MemoryBudgetManager::class.java)
    }

    if (budgetManager == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "MemoryBudgetManager system service is not available on this device.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    // High-yield cache target: large Bitmaps that can be evicted quickly under memory pressure.
    val previewCache = remember {
        object : LruCache<String, Bitmap>(MAX_PREVIEW_CACHE_BYTES) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }
    }

    var activeMode by remember { mutableStateOf(WorkloadMode.BASELINE) }
    var snapshot by remember { mutableStateOf(querySnapshot(budgetManager)) }
    var cacheBytes by remember { mutableIntStateOf(previewCache.size()) }
    var tileCounter by remember { mutableIntStateOf(0) }
    var overBudgetCount by remember { mutableIntStateOf(0) }
    val eventLogs = remember { mutableStateListOf<String>() }

    fun appendLog(message: String) {
        val time = DateFormat.getTimeInstance().format(Date())
        eventLogs.add(0, "[$time] $message")
        if (eventLogs.size > 20) {
            eventLogs.removeAt(eventLogs.lastIndex)
        }
    }

    fun refreshTelemetry() {
        // Query on demand at discrete milestones rather than polling in a continuous loop.
        snapshot = querySnapshot(budgetManager)
        cacheBytes = previewCache.size()
    }

    // Best practice for OnOverBudgetListener:
    // 1. Keep it fast to provide immediate relief.
    // 2. Avoid allocating new objects or spawning threads inside the callback.
    // 3. Target high-yield caches such as large Bitmaps or render buffers.
    val overBudgetListener = remember(budgetManager, previewCache) {
        MemoryBudgetManager.OnOverBudgetListener { budgetBytes ->
            Log.w(TAG, "Over-budget callback triggered (limit=${formatMb(budgetBytes)})")
            previewCache.evictAll()
            overBudgetCount += 1
            cacheBytes = 0
            snapshot = querySnapshot(budgetManager)
        }
    }

    DisposableEffect(lifecycleOwner, budgetManager, overBudgetListener, previewCache) {
        var isRegistered = false

        fun unregisterListener() {
            if (isRegistered) {
                try {
                    budgetManager.unregisterProcessOverBudgetListener(overBudgetListener)
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Process listener was not registered", e)
                } finally {
                    isRegistered = false
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!isRegistered) {
                        budgetManager.registerProcessOverBudgetListener(
                            Looper.getMainLooper(),
                            overBudgetListener,
                        )
                        isRegistered = true
                        refreshTelemetry()
                        appendLog("ON_START: Registered OnOverBudgetListener on main Looper.")
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    unregisterListener()
                    // Clear dynamic overrides and cached bitmaps when leaving the screen.
                    budgetManager.clearProcessBudget()
                    previewCache.evictAll()
                    cacheBytes = 0
                    activeMode = WorkloadMode.BASELINE
                    appendLog("ON_STOP: Unregistered listener and cleared dynamic process budget.")
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregisterListener()
            budgetManager.clearProcessBudget()
            previewCache.evictAll()
        }
    }

    fun applyDynamicBudget(targetBytes: Long, mode: WorkloadMode) {
        try {
            // Idempotent cross-process IPC: calling with the same value is a safe no-op.
            budgetManager.processBudgetBytes = targetBytes
            activeMode = mode
            if (mode == WorkloadMode.GALLERY) {
                previewCache.trimToSize(GALLERY_CACHE_BYTES)
            }
            refreshTelemetry()
            appendLog("Applied ${formatMb(targetBytes)} dynamic process budget (${mode.label}).")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to apply dynamic budget", e)
            appendLog("Rejected budget ${formatMb(targetBytes)}: ${e.message}")
        }
    }

    fun enterEditingCanvas() {
        // Restore the manifest ceiling (or unconstrained baseline if no manifest tag is set).
        // Calling clearProcessBudget() repeatedly is an idempotent no-op.
        budgetManager.clearProcessBudget()
        activeMode = WorkloadMode.BASELINE
        refreshTelemetry()
        appendLog("Cleared dynamic budget to restore baseline/manifest ceiling.")
    }

    fun testExceedingBudgetRejection() {
        val excessiveBudgetBytes = Long.MAX_VALUE / 2
        try {
            budgetManager.processBudgetBytes = excessiveBudgetBytes
        } catch (e: IllegalArgumentException) {
            appendLog(
                "Caught expected IllegalArgumentException for above-ceiling budget: ${e.message}",
            )
        }
    }

    fun allocatePreviewTile() {
        val nextId = tileCounter + 1
        tileCounter = nextId
        val bitmap = createBitmap(TILE_DIMENSION_PX, TILE_DIMENSION_PX)
        // Touch pixels so memory pages are faulted in and counted toward resident usage.
        Canvas(bitmap).drawColor(Color.rgb((nextId * 47) % 255, 120, 200))
        previewCache.put("tile_$nextId", bitmap)
        refreshTelemetry()
        appendLog("Cached 4 MB preview tile #$nextId (cache=${formatMb(cacheBytes.toLong())}).")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "App Memory Budgets (API 37.2)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Demonstrates lifecycle-driven dynamic budgets, on-demand telemetry, and " +
                    "high-yield cache eviction with MemoryBudgetManager.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "On-Demand Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TelemetryRow("Active Mode", activeMode.label)
                    TelemetryRow(
                        "Process Usage",
                        formatMb(snapshot.processUsageBytes),
                    )
                    TelemetryRow(
                        "Process Budget",
                        formatBudget(snapshot.processBudgetBytes),
                    )
                    TelemetryRow(
                        "Package Usage",
                        formatMb(snapshot.packageUsageBytes),
                    )
                    TelemetryRow(
                        "Package Budget",
                        formatBudget(snapshot.packageBudgetBytes),
                    )
                    TelemetryRow(
                        "Preview Bitmap Cache",
                        "${formatMb(cacheBytes.toLong())} / ${formatMb(MAX_PREVIEW_CACHE_BYTES.toLong())}",
                    )
                    TelemetryRow("Over-Budget Callbacks", overBudgetCount.toString())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            refreshTelemetry()
                            appendLog("Refreshed telemetry on demand.")
                        },
                    ) {
                        Text("Refresh Telemetry")
                    }
                }
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "1. Discrete Workload Transitions",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Update budgets only at discrete navigation or lifecycle " +
                            "milestones, never in tight loops or per-frame render routines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                applyDynamicBudget(GALLERY_BUDGET_BYTES, WorkloadMode.GALLERY)
                            },
                        ) {
                            Text("Gallery Mode (96 MB)")
                        }
                        FilledTonalButton(onClick = ::enterEditingCanvas) {
                            Text("Editing Canvas (Clear)")
                        }
                        OutlinedButton(
                            onClick = {
                                applyDynamicBudget(
                                    TIGHT_PRESSURE_TEST_BUDGET_BYTES,
                                    WorkloadMode.TIGHT_TEST,
                                )
                            },
                        ) {
                            Text("Tight Limit (48 MB)")
                        }
                        OutlinedButton(onClick = ::testExceedingBudgetRejection) {
                            Text("Test Ceiling Error")
                        }
                    }
                }
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "2. High-Yield Cache Simulation",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Allocate 4 MB ARGB_8888 Bitmap tiles to observe resident usage " +
                            "and proactive OnOverBudgetListener eviction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = ::allocatePreviewTile) {
                            Text("Cache 4 MB Bitmap Tile")
                        }
                        OutlinedButton(
                            onClick = {
                                previewCache.evictAll()
                                refreshTelemetry()
                                appendLog("Manually evicted all cached preview Bitmaps.")
                            },
                        ) {
                            Text("Evict Cache")
                        }
                    }
                }
            }
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Manifest & Runtime Best Practices",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "• Prefer <memory-budget> in AndroidManifest.xml (release builds " +
                            "only to avoid debug swap churn).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Order manifest clauses from general to specific: the last " +
                            "applicable clause takes effect.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Runtime budgets can only tighten the manifest ceiling; " +
                            "exceeding it throws IllegalArgumentException.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "• Keep OnOverBudgetListener fast, allocation-free, and focused " +
                            "on large caches (Bitmaps, render buffers, mapped files).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            Text(
                text = "Event Log",
                style = MaterialTheme.typography.titleMedium,
            )
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }

        if (eventLogs.isEmpty()) {
            item {
                Text(
                    text = "No events recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(eventLogs) { entry ->
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@RequiresApi(Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
private fun querySnapshot(budgetManager: MemoryBudgetManager): MemorySnapshot {
    return MemorySnapshot(
        processUsageBytes = budgetManager.processCurrentUsageBytes,
        packageUsageBytes = budgetManager.packageCurrentUsageBytes,
        processBudgetBytes = budgetManager.processBudgetBytes,
        packageBudgetBytes = budgetManager.packageBudgetBytes,
    )
}

@RequiresApi(Build.VERSION_CODES_FULL.CINNAMON_BUN_2)
private fun formatBudget(bytes: Long): String {
    return if (bytes == MemoryBudgetManager.LIMIT_IS_DISABLED) {
        "Unconstrained (LIMIT_IS_DISABLED)"
    } else {
        formatMb(bytes)
    }
}

private fun formatMb(bytes: Long): String {
    val mb = bytes / BYTES_PER_MB.toDouble()
    return String.format(Locale.US, "%.1f MB", mb)
}
