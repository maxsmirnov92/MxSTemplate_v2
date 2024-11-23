plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.demo"
}

dependencies {
    implementation(project(":feature:preferences:data"))
    implementation(project(":core:utils"))
    implementation(project(":core:android"))
    implementation(project(":core:ui"))
    implementation(project(":core:di"))
}