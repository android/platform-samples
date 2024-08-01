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
buildscript {
    dependencies {
        classpath("de.undercouch:gradle-download-task:4.1.2")
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false

    alias(libs.plugins.affectedmoduledetector)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.benManesVersions)

    id("com.example.platform")

}

versionCatalogUpdate {
    sortByKey.set(true)
    keep {
        keepUnusedVersions.set(true)
    }
}

affectedModuleDetector {
    baseDir = "${project.rootDir}"
    pathsAffectingAllModules = setOf(
        "gradle/libs.versions.toml",
    )
    excludedModules = setOf<String>()

    logFilename = "output.log"
    logFolder = "${rootProject.buildDir}/affectedModuleDetector"

    val baseRef = findProperty("affected_base_ref") as? String
    // If we have a base ref to diff against, extract the branch name and use it
    if (!baseRef.isNullOrEmpty()) {
        // Remove the prefix from the head.
        // TODO: need to support other types of git refs
        specifiedBranch = baseRef.replace("refs/heads/", "")
        compareFrom = "SpecifiedBranchCommit"
    } else {
        // Otherwise we use the previous commit. This is mostly used for commits to main.
        compareFrom = "PreviousCommit"
    }
}