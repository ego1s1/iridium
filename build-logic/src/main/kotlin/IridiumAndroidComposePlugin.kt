import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class IridiumAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // NOTE: modules must also apply `kotlin.compose` via alias in their own
            // plugins block; convention plugins cannot resolve external plugin ids.
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            // Compose compiler metrics + reports (stability audit trail for
            // the 3.6 pass): per-module build/compose-metrics and
            // build/compose-reports. Scoped to modules with the compose
            // compiler plugin; configured afterEvaluate so extension order
            // does not matter.
            afterEvaluate {
                if (pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose")) {
                    val metricsDir = layout.buildDirectory.dir("compose-metrics").get().asFile
                    val reportsDir = layout.buildDirectory.dir("compose-reports").get().asFile
                    val kotlin = extensions.findByType(
                        org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions::class.java,
                    )
                    // Fall through to the Android variant when the plain JVM
                    // options type is not registered.
                    val options = kotlin ?: extensions.findByName("kotlin")?.let {
                        it as? org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
                    }?.compilerOptions
                    options?.freeCompilerArgs?.addAll(
                        "-P",
                        "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                            metricsDir.absolutePath,
                        "-P",
                        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                            reportsDir.absolutePath,
                    )
                }
            }

            // New AGP DSL (AppExtension/LibraryExtension are deprecated and
            // break on AGP 9). Lenient afterEvaluate lookup: the Android
            // extension is registered by AGP after this plugin applies.
            afterEvaluate {
                @Suppress("UNCHECKED_CAST")
                (extensions.findByName("android") as? CommonExtension<*, *, *, *, *, *>)
                    ?.buildFeatures
                    ?.compose = true
            }

            dependencies {
                val bom = libs.findLibrary("androidx-compose-bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))
                add("implementation", libs.findLibrary("androidx-compose-ui").get())
                add("implementation", libs.findLibrary("androidx-compose-foundation").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}
