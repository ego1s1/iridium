plugins {
    // AGP and the Kotlin plugin are already on the build classpath through
    // build-logic (compileOnly), so they are applied by id without a version.
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

/**
 * Macrobenchmark harness. Measures a release-like build of :app (minified,
 * shrunk, debug-signed) and never ships.
 *
 * Run one benchmark at a time on an idle device:
 *   ./gradlew :benchmark:connectedPlayBenchmarkAndroidTest
 * Results land in benchmark/build/outputs/connected_android_test_additional_output/.
 */
android {
    namespace = "com.iridium.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Mirrors :app's `store` dimension; AGP matches test variants to app
    // variants by name, so the dimension and flavor names must line up.
    flavorDimensions += "store"
    productFlavors {
        create("play") {
            dimension = "store"
            buildConfigField("String", "TARGET_PACKAGE", "\"com.iridium.reader\"")
        }
        create("foss") {
            dimension = "store"
            buildConfigField("String", "TARGET_PACKAGE", "\"com.iridium.reader.foss\"")
        }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        // The Play build is what ships, so it is what gets profiled.
        variant.enable = variant.buildType == "benchmark" && variant.flavorName == "play"
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.junit)
}
