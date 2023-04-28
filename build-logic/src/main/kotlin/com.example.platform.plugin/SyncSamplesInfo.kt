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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class SyncSamplesInfo : DefaultTask() {

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun create() {
        val projectDirFile = projectDir.asFile.get()
        val samplesFolder = File(projectDirFile, "samples")
        val samples = mutableListOf<SampleInfo>()
        samplesFolder.walkBottomUp().forEach { file ->
            if (file.extension == "kt") {
                val text = file.readText()
                if (text.contains("@Sample")) {
                    val sample = SampleInfo(
                        name = text.findTag("name = "),
                        description = text.findTag("description = "),
                        path = file.path.removePrefix(samplesFolder.path + "/"),
                    )
                    samples.add(sample)
                }
            }
        }

        // Delete previously created file in case name changed
        File(projectDirFile, ".idea/runConfigurations").walkBottomUp().forEach { file ->
            if (file.isFile && file.name.startsWith("Sample-")) {
                file.delete()
            }
        }
        samples.forEach { sample ->
            createRunConfig(projectDirFile, sample.name)
        }

        createSamplesList(projectDirFile, samples)
    }

    private fun String.findTag(tag: String): String {
        val index = indexOf(tag)
        return if (index >= 0) {
            val startIndex = index + tag.length + 1
            substring(startIndex until indexOf("\"", startIndex))
        } else {
            ""
        }
    }
}

private data class SampleInfo(val name: String, val description: String, val path: String)

private fun createSamplesList(projectDir: File, samples: List<SampleInfo>) {
    val readme = buildString {
        append("# Available Samples\n\n")

        samples.sortedBy { it.name }.forEach {
            append("- [${it.name}](${it.path}):\n${it.description}\n")
        }
    }
    File(projectDir, "samples/README.md").apply {
        createNewFile()
        writeText(readme)
    }
}

private fun createRunConfig(projectDir: File, sampleName: String) {
    val ideaDir = File(projectDir, ".idea/runConfigurations")
    val startCommand =
        "<option name=\"ACTIVITY_EXTRA_FLAGS\" value=\"-e start &quot;$sampleName&quot;\" />"
    val defaultConfig = File(ideaDir, "app.xml").readText()
    val sampleConfig = defaultConfig
        .replace("name=\"app\"", "name=\"$sampleName\"")
        .replace("</configuration>", "$startCommand\n</configuration>")
    File(ideaDir, "Sample-${sampleName.replace(" ", "-")}.xml").apply {
        createNewFile()
        writeText(sampleConfig)
    }
}
