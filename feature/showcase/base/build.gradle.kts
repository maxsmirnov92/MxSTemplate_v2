plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.showcase"
}

dependencies {
    api(libs.showcaseView)
}