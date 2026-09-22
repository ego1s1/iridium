plugins {
    alias(libs.plugins.iridium.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.iridium.hilt)
}

android {
    namespace = "com.iridium.core.datastore"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
