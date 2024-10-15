plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.room)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}


android {
    namespace = "net.maxsmr.core.android"
}

dependencies {
    api("core_android.libs:commonutils-release-1.1.2@aar")
    api("core_android.libs:permissionchecker-release-1.2.1.0@aar")

    implementation(project(":core:di"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))
    implementation(project(":designsystem:shared_res"))

    implementation(libs.kotlinx.datetime)
    implementation(libs.picasso)

    //android
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
//    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.navigation.common.ktx)

    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.core.ktx)

    implementation(libs.kotlinx.serialization.json)

    //time
    implementation(libs.jodaTime)


    // Picasso
    implementation(libs.picasso)

    implementation(libs.decoro)

    api(libs.kittinunf.result)

    implementation(libs.androidx.datastore.preferences)

    // Добалено ля обработки HttpException
    implementation(libs.retrofit)

    implementation(libs.easypermissions)
}
