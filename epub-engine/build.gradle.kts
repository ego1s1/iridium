plugins {
    alias(libs.plugins.iridium.jvm.library)
}

dependencies {
    api(project(":epub-core"))
    testImplementation(libs.junit)
}
