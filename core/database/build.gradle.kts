plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.maxsmr.core.database"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:utils"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.datetime)

    implementation(libs.kotlinx.serialization.json)
}