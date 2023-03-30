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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * This task is used to create a new sample in the project. Given a path and a name it generates
 * the folder structure (if not there) and the sample target (using compose target) based on the
 * provided name.
 *
 * Usage:
 * $ ./gradlew createSample --path folder/optional-subfolder --name SampleName
 */
abstract class CreateSample : DefaultTask() {

    var samplePath: String = ""
        @Option(option = "path", description = "Defines the path for the sample")
        set
        @Input
        get

    var sampleName: String = ""
        @Option(option = "name", description = "Defines the sample module name")
        set
        @Input
        get

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun create() {
        require(samplePath.isNotEmpty()) {
            "Missing path, provide the path of the sample"
        }
        require(sampleName.isNotEmpty()) {
            "Missing sample module name"
        }

        // remove samples prefix if added in the path
        samplePath = samplePath.removePrefix("samples/")

        val projectDirFile = projectDir.asFile.get()
        val directory = File(projectDirFile, "samples/$samplePath")

        println("Creating $sampleName in $directory")

        val packagePath = samplePath.replace("/", ".").replace("-", ".")
        val samplePackage = "com.example.platform.${packagePath}"

        // Create module structure if it doesn't exists.
        if (!File(directory, "build.gradle.kts").exists()) {
            println("Creating build.gradle.kts for module")
            File(directory, "build.gradle.kts").apply {
                parentFile.mkdirs()
                createNewFile()
                writeText(sampleBuildTemplate(samplePackage))
            }
        }

        if (!File(directory, "README.md").exists()) {
            println("Creating README for module")
            File(directory, "README.md").apply {
                parentFile.mkdirs()
                createNewFile()
                writeText(readmeTemplate(sampleName))
            }
        }

        // Create sample target file using Compose target
        val sampleName = sampleName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val samplePath = "src/main/java/${samplePackage.replace(".", "/")}/$sampleName.kt"
        File(directory, samplePath).apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(sampleTemplate(samplePackage, sampleName))
        }

        println("Done! Sync and build project")
    }
}

private val projectLicense = """
/*
 * Copyright ${LocalDate.now().year} The Android Open Source Project
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
"""

private fun sampleBuildTemplate(samplePackage: String) = """
$projectLicense

plugins {
    id("com.example.platform.sample")
}

android {
    namespace = "$samplePackage"
}

dependencies {
    // Add samples specific dependencies
}
""".trimIndent()

private fun sampleTemplate(samplePackage: String, moduleName: String) = """
$projectLicense

package $samplePackage

import androidx.compose.runtime.Composable
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "$moduleName",
    description = "TODO: Add description"
)
@Composable
fun $moduleName() {
    // TODO: implement your sample. 
    // You can also use Activity or Fragment, simply tag them with the @Sample annotation
}
""".trimIndent()

private fun readmeTemplate(sampleName: String) = """
# $sampleName samples

// TODO: provide minimal instructions
```
""".trimIndent()