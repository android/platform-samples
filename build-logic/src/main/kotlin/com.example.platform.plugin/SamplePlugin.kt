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

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("UnstableApiUsage")
class SamplePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            val libs = extensions
                .getByType(VersionCatalogsExtension::class.java)
                .named("libs")

            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("com.google.devtools.ksp")
                apply("dagger.hilt.android.plugin")
                apply("kotlin-parcelize")
                apply<CommonConventionPlugin>()
            }

            pluginManager.withPlugin("java") {
                extensions.configure<JavaPluginExtension> {
                    toolchain {
                        it.languageVersion.set(JavaLanguageVersion.of(17))
                    }
                }
            }

            // TODO: remove when KSP starts respecting the Java/Kotlin toolchain
            tasks.withType(KotlinCompile::class.java).configureEach {
                it.kotlinOptions {
                    jvmTarget = "17"
                }
            }

            pluginManager.withPlugin("com.android.library") {
                configure<LibraryExtension> {
                    compileSdk = 34
                    defaultConfig {
                        minSdk = 21
                        @Suppress("DEPRECATION")
                        targetSdk = 34
                        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    }

                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_17
                        targetCompatibility = JavaVersion.VERSION_17
                    }

                    buildFeatures {
                        compose = true
                    }

                    composeOptions {
                        kotlinCompilerExtensionVersion =
                            libs.findVersion("composeCompiler").get().toString()
                    }
                }
            }

            dependencies {
                // Do not add the shared module to itself
                if (!project.displayName.contains("samples:base")) {
                    "implementation"(project(":samples:base"))
                }

                "implementation"(platform(libs.findLibrary("compose.bom").get()))
                "androidTestImplementation"(platform(libs.findLibrary("compose.bom").get()))

                "implementation"(libs.findLibrary("casa.base").get())
                "implementation"(libs.findLibrary("casa.ui").get())
                "ksp"(libs.findLibrary("casa.processor").get())

                "implementation"(libs.findLibrary("hilt.android").get())
                "ksp"(libs.findLibrary("hilt.compiler").get())

                "implementation"(libs.findLibrary("androidx.core").get())
                "implementation"(libs.findLibrary("androidx.fragment").get())
                "implementation"(libs.findLibrary("androidx.activity.compose").get())
                "implementation"(libs.findLibrary("compose.foundation.foundation").get())
                "implementation"(libs.findLibrary("compose.runtime.runtime").get())
                "implementation"(libs.findLibrary("compose.runtime.livedata").get())
                "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
                "implementation"(libs.findLibrary("compose.ui.ui").get())
                "implementation"(libs.findLibrary("compose.material3").get())
                "implementation"(libs.findLibrary("compose.material.iconsext").get())


                "implementation"(libs.findLibrary("coil.compose").get())
                "implementation"(libs.findLibrary("coil.video").get())

                "implementation"(libs.findLibrary("accompanist.permissions").get())

                "implementation"(libs.findLibrary("compose.ui.tooling.preview").get())
                "debugImplementation"(libs.findLibrary("compose.ui.tooling").get())

                "androidTestImplementation"(libs.findLibrary("androidx.test.core").get())
                "androidTestImplementation"(libs.findLibrary("androidx.test.runner").get())
            }
        }
    }
}
