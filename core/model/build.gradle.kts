plugins {
    alias(libs.plugins.iridium.jvm.library)
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    testImplementation(libs.junit)
}
