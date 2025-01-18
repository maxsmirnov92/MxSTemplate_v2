plugins {
    alias(libs.plugins.mxs.template.library.compose)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "net.maxsmr.core.ui.compose"
}

dependencies {
    implementation(project(":designsystem:compose"))
    implementation(project(":designsystem:shared_res"))
    implementation(project(":core:ui:base"))
    implementation(project(":core:android"))
    implementation(project(":core:utils"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
}