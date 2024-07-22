plugins {
    alias(libs.plugins.mxs.template.library)
}

android {
    namespace = "net.maxsmr.feature.mobile_services"
}

dependencies {
    implementation(project(":core:utils"))
    implementation(project(":core:android"))
    implementation(project(":core:ui"))
    implementation(project(":feature:preferences:data"))

    // ### В обоих реализациях можно обращаться к play-services-location
    implementation(libs.google.location)

    "huaweiImplementation"(libs.huawei.agconnect.core)
    "huaweiImplementation"(libs.huawei.location)

    "googleImplementation"(libs.google.play.app.update)
    "googleImplementation"(libs.google.play.app.update.ktx)
}