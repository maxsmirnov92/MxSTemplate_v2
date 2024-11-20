plugins {
    alias(libs.plugins.mxs.template.feature.data)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.notification_reader.data"
}

dependencies {
    implementation(project(":feature:preferences:data"))
    implementation(project(":feature:download:data"))
    implementation(project(":core:android"))
    implementation(project(":core:database"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
}