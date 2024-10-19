plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.camera"
}

dependencies {
    implementation(project(":core:di"))
    implementation(project(":feature:preferences:data"))
    implementation(project(":feature:mobile_services"))

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.camera.lifecycle)

    "googleImplementation"(libs.google.mlkit.text.recognition)
    "huaweiImplementation"(libs.huawei.ml.computer.vision.ocr)
    "huaweiImplementation"(libs.huawei.ml.computer.vision.ocr.latin.model)
}