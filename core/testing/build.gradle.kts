plugins {
    alias(libs.plugins.iridium.jvm.library)
}

dependencies {
    implementation(project(":core:model"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
