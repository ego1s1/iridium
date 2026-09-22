plugins {
    alias(libs.plugins.iridium.android.library)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.iridium.feature.onboarding.api"

android {
    namespace = "com.iridium.feature.onboarding.api"
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.serialization.json)
    api(libs.androidx.navigation.compose)
}
