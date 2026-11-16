# Memory Budget Manager Sample

This sample demonstrates how to declare and manage application memory budgets on **Android 17 QPR2 (API level 37.2 / `Build.VERSION_CODES_FULL.CINNAMON_BUN_2`)** and higher using `<memory-budget>` manifest declarations and the runtime [`MemoryBudgetManager`](https://developer.android.com/reference/android/app/MemoryBudgetManager) API.

For full guidance, see [Set app memory budgets](https://developer.android.com/topic/performance/memory/app-memory-budgets).

## What is demonstrated

1. **Manifest-declared budgets (`<memory-budget>`)** — Primary, recommended approach documented in [AndroidManifest.xml](src/main/AndroidManifest.xml):
   - Baseline budget (`android:maxMb`).
   - Process-state budgets (`android:state="foreground|perceptible|background"`).
   - Display scaling (`android:additionalMbPerDensity` or `android:additionalBytesPerDisplayPixel`).
   - Form-factor overrides (`android:feature="watch|automotive|leanback"`), ordered from general to specific because the **last applicable clause wins**.
   - **Release-only best practice**: Apply `<memory-budget>` only in release (optimized) builds (for example, via `src/release/AndroidManifest.xml`) so unoptimized debug instrumentation and tests do not experience swap churn.

2. **Runtime SDK API (`MemoryBudgetManager`)** — Secondary dynamic option in [MemoryBudgetSample.kt](src/main/java/com/example/platform/performance/memory/MemoryBudgetSample.kt):
   - **SDK gating**: Uses `Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2`.
   - **On-demand telemetry**: Queries `processCurrentUsageBytes`, `packageCurrentUsageBytes`, `processBudgetBytes`, and `packageBudgetBytes` (handling `LIMIT_IS_DISABLED`) on demand rather than polling in continuous loops.
   - **Discrete workload transitions**: Tightens the process budget (`processBudgetBytes = ...`) for lightweight workflows (e.g., gallery browsing), clears it (`clearProcessBudget()`) when returning to heavy workloads (e.g., editing canvas), and catches `IllegalArgumentException` when exceeding the manifest/system ceiling.
   - **Idempotency**: Calling `clearProcessBudget()` or setting the same budget value repeatedly is a safe no-op, intended for discrete lifecycle/navigation milestones rather than tight render loops.
   - **Over-budget callbacks (`OnOverBudgetListener`)**: Registers on `ON_START` and unregisters on `ON_STOP`. Callbacks stay fast, avoid allocations or spawning threads, and evict high-yield targets (`LruCache<String, Bitmap>`).

## Inspecting budgets and process state via ADB

Inspect active process state and importance:

```bash
adb shell dumpsys activity processes com.example.platform
```

Inspect the active enforced memory budget, limit source, and resident usage:

```bash
adb shell dumpsys meminfo com.example.platform
```

You can also monitor the app's memory footprint and OOM score adjustments continuously using [android_mem_monitor.py](../../../utilities/android_mem_monitor.py).

## License

```
Copyright 2026 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
