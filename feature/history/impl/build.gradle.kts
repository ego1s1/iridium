plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.iridium.android.feature)
    alias(libs.plugins.iridium.android.store)
}

group = "com.iridium.feature.history.impl"

android {
    namespace = "com.iridium.feature.history.impl"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
}
