plugins {
    alias(libs.plugins.mxs.template.feature.view)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.webview.ui"
}

dependencies {
    api(project(":feature:webview:data"))
    implementation(project(":feature:rate"))
}