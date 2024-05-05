plugins {
    alias(libs.plugins.mxs.template.feature.ui)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.feature.address_sorter.ui"
}

dependencies {
    api(project(":feature:address_sorter:data"))
    implementation(project(":core:di"))
    implementation(project(":feature:download:data"))
    implementation(project(":feature:preferences:ui"))

    implementation(libs.androidx.datastore.preferences)
}