plugins {
    alias(libs.plugins.iridium.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.iridium.hilt)
}

android {
    namespace = "com.iridium.core.database"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.turbine)
    testImplementation(project(":core:testing"))
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
