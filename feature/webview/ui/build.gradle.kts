plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.webview.ui"
}

dependencies {
    implementation(project(":core:di"))

    api(project(":feature:webview:data"))
}