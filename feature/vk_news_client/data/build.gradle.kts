plugins {
    alias(libs.plugins.mxs.template.feature.data)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vk.id.manifest.placeholders)
}

android {
    namespace = "net.maxsmr.feature.vk_news_client.data"
}

dependencies {
    implementation(project(":core:android"))

    api(libs.vk.id)

    implementation(libs.kotlinx.serialization.json)
}