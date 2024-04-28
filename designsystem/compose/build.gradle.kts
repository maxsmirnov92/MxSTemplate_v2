plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.library.compose)
}

android {
    namespace = "net.maxsmr.designsystem.compose"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)

    implementation(project(":designsystem:shared_res"))
}