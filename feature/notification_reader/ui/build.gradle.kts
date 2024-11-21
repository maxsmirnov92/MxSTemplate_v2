plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.notification_reader.ui"
}

dependencies {
    api(project(":feature:notification_reader:data"))
    implementation(project(":core:di"))

    implementation(libs.kotlinx.datetime)
}