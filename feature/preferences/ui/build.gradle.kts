plugins {
    alias(libs.plugins.mxs.template.feature.view)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.preferences.ui"
}

dependencies {
    api(project(":feature:preferences:data"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
}