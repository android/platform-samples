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
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Platform Samples"
include(":app")

include(":shared")
include(":samples:accessibility")
include(":samples:camera:camera2")
include(":samples:connectivity:audio")
include(":samples:connectivity:bluetooth:ble")
include(":samples:connectivity:bluetooth:companion")
include(":samples:connectivity:callnotification")
include(":samples:connectivity:telecom")
include(":samples:graphics:pdf")
include(":samples:graphics:ultrahdr")
include(":samples:location")
include(":samples:media:ultrahdr")
include(":samples:media:video")
include(":samples:privacy:data")
include(":samples:privacy:permissions")
include(":samples:privacy:transparency")
include(":samples:storage")
include(":samples:user-interface:appwidgets")
include(":samples:user-interface:constraintlayout")
include(":samples:user-interface:draganddrop")
include(":samples:user-interface:haptics")
include(":samples:user-interface:live-updates")
include(":samples:user-interface:picture-in-picture")
include(":samples:user-interface:predictiveback")
include(":samples:user-interface:quicksettings")
include(":samples:user-interface:share")
include(":samples:user-interface:text")
include(":samples:user-interface:window-insets")
include(":samples:user-interface:windowmanager")