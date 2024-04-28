plugins {
    alias(libs.plugins.mxs.template.feature.data)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.address_sorter.data"
}

dependencies {
    implementation(project(":feature:preferences:data"))
    implementation(project(":core:android"))

    implementation(libs.kotlinx.serialization.json)
}