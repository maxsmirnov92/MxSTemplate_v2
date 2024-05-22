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

    implementation(project(":feature:preferences:ui"))
    implementation(project(":core:di"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:utils"))
    implementation(project(":core:android"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
}