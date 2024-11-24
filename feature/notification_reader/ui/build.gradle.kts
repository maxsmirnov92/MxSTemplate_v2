plugins {
    alias(libs.plugins.mxs.template.feature.view)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.notification_reader.ui"
}

dependencies {
    api(project(":feature:notification_reader:data"))
    implementation(project(":feature:preferences:ui"))
    implementation(project(":feature:demo"))
    implementation(project(":core:di"))

    implementation(libs.kotlinx.datetime)
}