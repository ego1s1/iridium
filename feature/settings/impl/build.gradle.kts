plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.iridium.android.feature)
    alias(libs.plugins.iridium.android.store)
}

group = "com.iridium.feature.settings.impl"

android {
    namespace = "com.iridium.feature.settings.impl"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    // Drive sign-in lives in :core:data behind DriveSignIn, so this module
    // needs no vendor SDK.
    implementation(libs.kotlinx.serialization.json)
}
