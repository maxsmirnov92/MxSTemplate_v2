plugins {
    alias(libs.plugins.mxs.template.library)
}

android {
    namespace = "net.maxsmr.mobile_services"
}

dependencies {
    implementation(project(":core:utils"))
    implementation(project(":core:android"))

    // ### В обоих реализациях можно обращаться к play-services-location
    implementation(libs.google.location)

    "huaweiImplementation"(libs.huawei.agconnect.core)
    "huaweiImplementation"(libs.huawei.location)
}