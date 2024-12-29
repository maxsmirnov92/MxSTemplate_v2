plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.maxsmr.core.ui"
}

dependencies {
    // TODO по непонятным причинам api из core_utils через core_android не видит в этом модуле
    api("core_utils.libs:commonutils-jre-1.1@jar")

    implementation(project(":designsystem:shared_res"))
    implementation(project(":core:di"))
    implementation(project(":core:android"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))

    //paging
    api(libs.androidx.paging.runtime)
    api(libs.androidx.paging.runtime.ktx)

    //Navigation
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)

    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
//    implementation(libs.androidx.lifecycle.extensions)

    api(libs.kotlinx.coroutines.android)
}