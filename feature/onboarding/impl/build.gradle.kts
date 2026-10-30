plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.iridium.android.feature)
    // Test sources consume :core:test-fakes, which exposes the flavored
    // :core:data API, so this module needs the dimension too.
    alias(libs.plugins.iridium.android.store)
}

group = "com.iridium.feature.onboarding.impl"

android {
    namespace = "com.iridium.feature.onboarding.impl"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:onboarding:api"))
    testImplementation(libs.turbine)
}
