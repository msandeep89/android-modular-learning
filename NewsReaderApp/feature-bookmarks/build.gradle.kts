// Dynamic feature modules use com.android.dynamic-feature plugin.
// Key difference from library modules:
//   - They depend on :app (not :core directly)
//   - They appear in :app's dynamicFeatures list
//   - They are NOT included in the base APK
plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.sandeep.newsreader.feature.bookmarks"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}

dependencies {
    // Dynamic features depend on :app — this gives access to :core transitively
    implementation(project(":app"))
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment.ktx)
    testImplementation(libs.junit)
}
