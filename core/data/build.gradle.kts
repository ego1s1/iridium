plugins {
    alias(libs.plugins.iridium.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.iridium.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.iridium.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":epub-core"))
    implementation(project(":epub-engine"))
    implementation(project(":epub-native"))
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.ktx)
    testImplementation(project(":core:testing"))
}
