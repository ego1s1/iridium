plugins {
    `kotlin-dsl`
}

group = "com.iridium.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    // Plugin markers needed at convention-application time (pluginManager
    // cannot resolve external ids from compileOnly): ksp + hilt ride the
    // build-logic runtime classpath so iridium.hilt applies them itself.
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.hilt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "iridium.android.application"
            implementationClass = "IridiumAndroidApplicationPlugin"
        }
        register("androidLibrary") {
            id = "iridium.android.library"
            implementationClass = "IridiumAndroidLibraryPlugin"
        }
        register("androidFeature") {
            id = "iridium.android.feature"
            implementationClass = "IridiumAndroidFeaturePlugin"
        }
        register("androidCompose") {
            id = "iridium.android.compose"
            implementationClass = "IridiumAndroidComposePlugin"
        }
        register("androidStore") {
            id = "iridium.android.store"
            implementationClass = "IridiumAndroidStorePlugin"
        }
        register("hilt") {
            id = "iridium.hilt"
            implementationClass = "IridiumHiltPlugin"
        }
        register("jvmLibrary") {
            id = "iridium.jvm.library"
            implementationClass = "IridiumJvmLibraryPlugin"
        }
    }
}
