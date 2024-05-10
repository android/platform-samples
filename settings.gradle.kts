import java.util.Properties

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
        // Uncomment this to use a snapshot version of casa-android.
        // maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Uncomment this to use a snapshot version of casa-android.
        // maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven {
            url = uri("https://androidx.dev/snapshots/builds/11809947/artifacts/repository")
        }
    }
}

rootProject.name = "Platform Samples"
include(":app")

// Define the samples to load
var samples = emptyList<String>()

// If the local.properties define specific samples use those only
val propertiesFile = file("local.properties")
if (propertiesFile.exists()) {
    val properties = Properties()
    properties.load(propertiesFile.inputStream())
    if (properties.containsKey("target.samples")) {
        // Specify the sample module name (e.g :samples:privacy:permissions) or comma separated ones
        samples = listOf(":samples:base") + properties["target.samples"].toString().split(",")
    }
}

// Dynamically include samples under /app-catalog/samples/ folder if no target.samples were defined
if (samples.isEmpty()) {
    samples = buildList {
        val separator = File.separator
        // Find all build.gradle files under samples folder
        settingsDir.walk()
            .filter { it.name == "build.gradle" || it.name == "build.gradle.kts" }
            .filter {
                val relativePath = if (it.isAbsolute) {
                    it.path.substring(settingsDir.path.length)
                } else {
                    it.path
                }
                relativePath.contains("${separator}samples${separator}")
            }
            .map { it.parent.substring(rootDir.path.length) }
            .forEach {
                add(it.replace(separator, ":"))
            }
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
