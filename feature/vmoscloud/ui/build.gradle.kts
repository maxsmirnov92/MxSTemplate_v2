plugins {
    alias(libs.plugins.mxs.template.feature.view)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.vmoscloud.ui"
}

dependencies {
    api(project(":feature:vmoscloud:data"))
    implementation(project(":core:di"))
    implementation(project(":feature:preferences:ui"))

    implementation(libs.androidx.datastore.preferences)
}