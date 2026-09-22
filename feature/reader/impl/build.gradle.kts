plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.iridium.android.feature)
}

group = "com.iridium.feature.reader.impl"

android {
    namespace = "com.iridium.feature.reader.impl"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:reader:api"))
}
