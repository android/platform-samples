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

package com.example.platform.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CreateRunConfig : DefaultTask() {

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun create() {
        val projectDirFile = projectDir.asFile.get()
        val samplesFolder = File(projectDirFile, "samples")
        val tag = "name = "
        val sampleNames = mutableListOf<String>()
        samplesFolder.walkBottomUp().forEach { file ->
            if (file.extension == "kt") {
                val text = file.readText()
                if (text.contains("@Sample")) {
                    val index = text.indexOf(tag)
                    if (index >= 0) {
                        val startIndex = index + tag.length + 1
                        sampleNames.add(
                            text.substring(startIndex until text.indexOf("\"", startIndex))
                        )
                    }
                }
            }
        }

        sampleNames.forEach { sampleName ->
            createRunConfig(projectDirFile, sampleName)
        }

        val ideaDir = File(projectDirFile, ".idea/runConfigurations")
        Runtime.getRuntime().exec("git add $ideaDir")

        println("Done! Sync and build project")
    }
}

fun createRunConfig(projectDir: File, sampleName: String) {
    val ideaDir = File(projectDir, ".idea/runConfigurations")
    val startCommand =
        "<option name=\"ACTIVITY_EXTRA_FLAGS\" value=\"-e start &quot;$sampleName&quot;\" />"
    val defaultConfig = File(ideaDir, "app.xml").readText()
    val sampleConfig = defaultConfig
        .replace("name=\"app\"", "name=\"$sampleName\"")
        .replace("</configuration>", "$startCommand\n</configuration>")
    File(ideaDir, "$sampleName.xml").apply {
        createNewFile()
        writeText(sampleConfig)
    }
}