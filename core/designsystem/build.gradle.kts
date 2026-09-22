plugins {
    alias(libs.plugins.iridium.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.iridium.android.compose)
}

android {
    namespace = "com.iridium.core.designsystem"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
}
