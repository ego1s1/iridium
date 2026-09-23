import java.io.File
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.iridium.android.library)
}

android {
    namespace = "com.iridium.epub.nativecore"

    ndkVersion = "27.2.12479018"

    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
                cppFlags += listOf("-std=c++17", "-fexceptions")
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":epub-core"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(project(":epub-engine"))
    // Real org.json on the unit-test classpath: android.jar only ships stubs.
    testImplementation(libs.org.json)
}

/**
 * Compiles the same C++ core for the host so JVM unit tests can exercise the
 * native parser end to end (`libiridium_epub.dylib` / `.so`, loaded via
 * `java.library.path`). Keeps the native path verifiable without a device.
 */
val hostNativeDir: Provider<Directory> = layout.buildDirectory.dir("host-native")

/** C++ driver used for the host build; overridable via CXX. */
val hostCompiler: String = providers.environmentVariable("CXX").orNull?.takeIf { it.isNotBlank() }
    ?: "clang++"

/** False when no host C++ compiler is present, so the task skips cleanly. */
val hostCompilerAvailable: Boolean = runCatching {
    ProcessBuilder(hostCompiler, "--version")
        .redirectErrorStream(true)
        .start()
        .waitFor() == 0
}.getOrDefault(false)

val compileHostNative = tasks.register<Exec>("compileHostNative") {
    val outputDir = hostNativeDir.get().asFile
    val source = file("src/main/cpp/iridium_epub.cpp")
    val javaHome = System.getProperty("java.home")

    inputs.file(source)
    outputs.dir(outputDir)
    onlyIf { hostCompilerAvailable }

    doFirst { outputDir.mkdirs() }

    val isMac = System.getProperty("os.name").startsWith("Mac")
    val extension = if (isMac) "dylib" else "so"
    val sharedFlag = if (isMac) "-dynamiclib" else "-shared"
    val flags = mutableListOf(
        "-std=c++17", "-fPIC", "-O1", sharedFlag,
        "-I", "$javaHome/include",
        "-I", "$javaHome/include/darwin",
        "-I", "$javaHome/include/linux",
        source.absolutePath,
        "-lz",
        "-o", File(outputDir, "libiridium_epub.$extension").absolutePath,
    )

    // Must be the C++ driver: `cc` would not link libc++.
    commandLine(listOf(hostCompiler) + flags)
}

tasks.withType<Test>().configureEach {
    dependsOn(compileHostNative)
    val dir = hostNativeDir.get().asFile.absolutePath
    val existing = System.getProperty("java.library.path").orEmpty()
    systemProperty(
        "java.library.path",
        if (existing.isEmpty()) dir else "$existing${File.pathSeparator}$dir",
    )
    // Reading FileDescriptor.fd from the test JVM needs the internal field.
    jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
}
