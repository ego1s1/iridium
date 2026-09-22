plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.iridium.android.feature)
}

group = "com.iridium.feature.settings.impl"

android {
    namespace = "com.iridium.feature.settings.impl"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
}
