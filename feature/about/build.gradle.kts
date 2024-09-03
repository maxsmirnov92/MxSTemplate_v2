plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.about"
}

dependencies {
    implementation(project(":core:di"))
//    implementation(project(":feature:rate"))
//    implementation(project(":feature:mobile_services"))
    implementation(project(":feature:preferences:data"))
}