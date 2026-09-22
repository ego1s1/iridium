plugins {
    alias(libs.plugins.iridium.android.library)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.iridium.feature.reader.api"

android {
    namespace = "com.iridium.feature.reader.api"
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.serialization.json)
    api(libs.androidx.navigation.compose)
}
