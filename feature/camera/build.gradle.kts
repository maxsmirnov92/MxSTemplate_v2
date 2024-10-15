plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.camera"
}

dependencies {
    implementation(project(":core:di"))
    implementation(project(":feature:preferences:data"))
}