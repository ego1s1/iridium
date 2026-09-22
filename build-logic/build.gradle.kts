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
    compileOnly(libs.ksp.gradlePlugin)
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
