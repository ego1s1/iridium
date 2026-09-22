plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.iridium.android.feature)
}

group = "com.iridium.feature.onboarding.impl"

android {
    namespace = "com.iridium.feature.onboarding.impl"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:onboarding:api"))
}
