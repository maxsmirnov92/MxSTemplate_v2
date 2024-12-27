plugins {
    alias(libs.plugins.mxs.template.feature.view)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.showcase.settings"
}

dependencies {
    api(project(":feature:showcase:base"))
    implementation(project(":feature:preferences:ui"))
}