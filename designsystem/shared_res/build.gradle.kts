plugins {
    alias(libs.plugins.mxs.template.library)
}

android {
    namespace = "net.maxsmr.designsystem.shared_res"
}

dependencies {
    implementation(libs.google.material)
}