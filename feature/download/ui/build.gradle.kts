plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.download.ui"
}

dependencies {
    api(project(":feature:download:data"))
    implementation(project(":core:di"))
    implementation(project(":feature:webview:ui"))
    implementation(project(":feature:preferences:ui"))
}