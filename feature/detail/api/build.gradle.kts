plugins {
    alias(libs.plugins.iridium.android.library)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.iridium.feature.detail.api"

android {
    namespace = "com.iridium.feature.detail.api"
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.serialization.json)
    api(libs.androidx.navigation.compose)
}
