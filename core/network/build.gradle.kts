plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.maxsmr.core.network"
}

dependencies {
    implementation("core_android.libs:commonutils-release-1.1.3@aar")

    implementation(project(":core:domain"))
    implementation(project(":core:utils"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.kotlinx.coroutines.core)

    // Network
    api(libs.okhttp)
//    implementation(libs.volley)
    api(libs.retrofit)
    api(libs.retrofit.converter.scalars)
    implementation(libs.retrofit.kotlinx.serializationConverter)
    implementation(libs.okhttp.loggingInterceptor)

    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json.okio)
}