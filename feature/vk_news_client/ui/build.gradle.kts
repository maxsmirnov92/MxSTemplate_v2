plugins {
    alias(libs.plugins.mxs.template.feature.compose)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vk.id.manifest.placeholders)
}

android {
    namespace = "net.maxsmr.feature.vk_news_client.ui"
}

dependencies {
    api(project(":feature:vk_news_client:data"))
    implementation(project(":core:di"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.vk.onetap.compose)
}