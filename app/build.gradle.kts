plugins {
    alias(libs.plugins.iridium.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.iridium.android.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.iridium.hilt)
}

android {
    namespace = "com.iridium.app"

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.iridium.reader"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":feature:onboarding:impl"))
    implementation(project(":feature:library:impl"))
    implementation(project(":feature:history:impl"))
    implementation(project(":feature:detail:impl"))
    implementation(project(":feature:reader:impl"))
    implementation(project(":feature:settings:impl"))
    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:detail:api"))
    implementation(project(":feature:reader:api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":epub-core"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":core:test-fakes"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
