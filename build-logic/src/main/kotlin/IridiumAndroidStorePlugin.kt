import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Adds the publishing-channel flavor dimension (`play` / `foss`) to a module.
 *
 * Every module that consumes a flavored module has to declare the same
 * dimension: variant-aware resolution cannot guess which flavor an unflavored
 * consumer wants. Modules that ship channel-specific sources or dependencies
 * apply this plugin, and the app applies it so its own variants line up.
 */
class IridiumAndroidStorePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { applyStoreFlavors() }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { applyStoreFlavors() }
            }
        }
    }
}

/** Shared names: `store` splits the app into `play` and `foss` channels. */
internal const val STORE_DIMENSION = "store"
internal const val STORE_PLAY = "play"
internal const val STORE_FOSS = "foss"

private fun ApplicationExtension.applyStoreFlavors() {
    flavorDimensions += STORE_DIMENSION
    productFlavors.create(STORE_PLAY) { dimension = STORE_DIMENSION }
    productFlavors.create(STORE_FOSS) {
        dimension = STORE_DIMENSION
        // Separate package so the store build and the F-Droid build can be
        // installed side by side. F-Droid signs with its own key, so an
        // in-place migration between the two is not possible either way.
        applicationIdSuffix = ".foss"
    }
}

private fun LibraryExtension.applyStoreFlavors() {
    flavorDimensions += STORE_DIMENSION
    productFlavors.create(STORE_PLAY) { dimension = STORE_DIMENSION }
    productFlavors.create(STORE_FOSS) { dimension = STORE_DIMENSION }
}
