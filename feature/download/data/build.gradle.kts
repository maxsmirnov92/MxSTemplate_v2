plugins {
    alias(libs.plugins.mxs.template.feature.data)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.download.data"
}

dependencies {
    implementation(project(":feature:preferences:data"))
    implementation(project(":core:android"))
    implementation(project(":core:ui"))

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
}