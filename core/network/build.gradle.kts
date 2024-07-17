plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.core.network"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:utils"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.kotlinx.coroutines.core)

    // Network
    implementation(libs.okhttp)
//    implementation(libs.volley)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serializationConverter)
    implementation(libs.okhttp.loggingInterceptor)

    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json.okio)
}