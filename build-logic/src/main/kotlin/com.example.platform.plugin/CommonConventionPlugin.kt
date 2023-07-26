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

package com.example.platform.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.gradle.kotlin.dsl.configure

class CommonConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.whenTaskAdded { task ->
                if (task.name == "connectedDebugAndroidTest") {
                    task.finalizedBy("uninstallDebugAndroidTest")
                }
            }

            configurations.configureEach {
                it.resolutionStrategy.eachDependency { details ->
                    // Make sure that we"re using the Android version of Guava
                    if (details.requested.group == "com.google.guava"
                        && details.requested.module.name == "guava"
                        && details.requested.version?.contains("jre") == true) {
                        details.useVersion(details.requested.version!!.replace("jre", "android"))
                    }
                }
            }

            pluginManager.withPlugin("java") {
                extensions.configure<JavaPluginExtension> {
                    toolchain {
                        it.languageVersion.set(JavaLanguageVersion.of(17))
                    }
                }
            }

            pluginManager.withPlugin("org.jebrains.kotlin.jvm") {
                extensions.configure<KotlinJvmOptions> {
                    // Treat all Kotlin warnings as errors
                    allWarningsAsErrors = true
                    // Set JVM target to 17
                    jvmTarget = "17"
                    // Allow use of @OptIn
                    freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
                    // Enable default methods in interfaces
                    freeCompilerArgs += "-Xjvm-default=all"
                }
            }
        }
    }
}