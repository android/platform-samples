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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.util.Locale

abstract class CreateSample : DefaultTask() {

    var sampleName: String = ""
        @Option(option = "name", description = "Defines the sample module name")
        set
        @Input
        get

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun create() {
        require(sampleName.isNotEmpty()) {
            "Missing sample module name"
        }
        val samplePackage = "com.example.platform.${sampleName}"
        val directory = File(projectDir.asFile.get(), "samples/$sampleName")

        println("Creating $sampleName in $directory")

        File(directory, "build.gradle").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(sampleBuildTemplate(samplePackage))
        }

        val sampleName = sampleName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val samplePath = "src/main/java/${samplePackage.replace(".", "/")}/$sampleName.kt"
        File(directory, samplePath).apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(sampleTemplate(samplePackage, sampleName))
        }

        File(directory, "README.md").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(readmeTemplate(sampleName))
        }

        Runtime.getRuntime().exec("git add $directory")

        println("Done! Sync and build project")
    }
}

private const val PLUGIN_PATH = "\$rootDir/gradle/sample-build.gradle"
private fun sampleBuildTemplate(samplePackage: String) = """
apply from: "$PLUGIN_PATH"

android {
    namespace '$samplePackage'
}

dependencies {
    // Add samples specific dependencies
}
""".trimIndent()

private fun sampleTemplate(samplePackage: String, moduleName: String) = """
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

// TODO

## License

```
Copyright 2022 The Android Open Source Project
 
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
""".trimIndent()