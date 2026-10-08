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
