plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.showcase"
}

dependencies {
    implementation(project(":core:android"))
    implementation(project(":core:ui:base"))
    api(libs.showcaseView)
}