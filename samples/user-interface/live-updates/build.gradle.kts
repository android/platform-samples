plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
}

android {
    namespace = "com.example.platform.ui.live_updates"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
    lint {
        targetSdk = 37
    }
    testOptions {
        targetSdk = 37
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3.android)
}
