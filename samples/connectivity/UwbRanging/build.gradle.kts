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

//@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.example.platform.connectivity.uwb"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/proto")
        }
    }
}

protobuf {
    // Configures the protoc compiler
    protoc {
        // Automatically download protoc from Maven Central
        artifact = "com.google.protobuf:protoc:3.25.3"
    }

    // Configures the code generation tasks
    generateProtoTasks {
        all().forEach { task ->
            // Generate standard Kotlin data classes from your .proto files
            task.plugins {
                create("kotlin")
                create("java")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.uwb)
    implementation(project(mapOf("path" to ":samples:connectivity:audio")))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(project(":shared"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material.iconsext)
    implementation(libs.kotlin.coroutines.play)
    implementation("com.google.android.gms:play-services-nearby:19.2.0")
    implementation("com.google.protobuf:protobuf-java:3.25.3")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.3")
    implementation("androidx.datastore:datastore:1.0.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.firebase.crashlytics.buildtools)


    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.ui.test.manifest)

    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.junit4)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
