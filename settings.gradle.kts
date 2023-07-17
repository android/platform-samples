/*
 * Copyright 2022 The Android Open Source Project
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

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Using SNAPSHOTS for Telecom SDK. This should be removed once telecom SDK is stable
        maven { url = uri("https://androidx.dev/snapshots/builds/10506499/artifacts/repository") }
    }
}

rootProject.name = "Platform Samples"
include(":app")

// Dynamically include samples under /app-catalog/samples/ folder
val samples = buildList {
    val separator = File.separator
    // Find all build.gradle files under samples folder
    settingsDir.walk()
        .filter { it.name == "build.gradle" || it.name == "build.gradle.kts" }
        .filter { it.path.contains("${separator}samples${separator}") }
        .map { it.parent.substring(rootDir.path.length) }
        .forEach {
            add(it.replace(separator, ":"))
        }
}



// include all available samples and store it in :app project extras.
println("Included samples: $samples")
include(*samples.toTypedArray())
gradle.beforeProject {
    if (name == "app") {
        extra["samples"] = samples
    }
}
