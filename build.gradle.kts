plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.binary.compatibility.validator) apply false
}

// Hilt's aggregator worker loads JavaPoet parent-first; AGP's bundled jetifier ships
// JavaPoet 1.10.0 (missing ClassName.canonicalName). Pinning 1.13.0 on the root
// buildscript classpath (ancestor of plugin loaders) makes the compatible version win.
buildscript {
    dependencies {
        classpath(libs.javapoet)
    }
}

allprojects {
    group = "com.iridium"
    version = "1.0.0"
}

// Static analysis runs everywhere from one config, applied here rather than in
// each module so a new module cannot silently skip the gate.
subprojects {
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // Set on the task itself: the extension's `config` is not always
        // propagated to tasks created from convention plugins.
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        reports {
            html.required.set(true)
            xml.required.set(false)
            sarif.required.set(false)
            txt.required.set(true)
        }
    }
}
