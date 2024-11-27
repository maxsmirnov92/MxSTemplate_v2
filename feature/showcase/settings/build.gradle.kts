plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.showcase.settings"
}

dependencies {
    api(project(":feature:showcase:base"))
    implementation(project(":feature:preferences:ui"))
}