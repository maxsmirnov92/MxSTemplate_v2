plugins {
    alias(libs.plugins.mxs.template.feature.data)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.webview.data"
}

dependencies {
    implementation(project(":core:android"))
    implementation(project(":core:ui"))

    api(libs.okhttp)
}