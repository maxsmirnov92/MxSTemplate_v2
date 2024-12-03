plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.hilt)
}

android {
    namespace = "net.maxsmr.feature.notification_reader.ui"
}

dependencies {
    api(project(":feature:notification_reader:data"))
    implementation(project(":feature:download:data"))
    implementation(project(":feature:preferences:ui"))
    implementation(project(":feature:demo"))
    implementation(project(":feature:showcase:settings"))
    implementation(project(":core:di"))

    implementation(libs.kotlinx.datetime)
}