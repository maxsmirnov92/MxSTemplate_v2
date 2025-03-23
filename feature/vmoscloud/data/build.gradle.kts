plugins {
    alias(libs.plugins.mxs.template.feature.data)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.vmoscloud.data"
}

dependencies {
    implementation(project(":feature:preferences:data"))
    implementation(project(":core:android"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.armcloudsdk.armcloudsdkv3)
}