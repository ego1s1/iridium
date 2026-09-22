plugins {
    alias(libs.plugins.iridium.android.library)
}

android {
    namespace = "com.iridium.core.fakes"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:datastore"))
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.documentfile)
}
