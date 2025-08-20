plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
}

android {
    namespace = "com.example.platform.ui.live_updates"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        targetSdk = 36
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
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