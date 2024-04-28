plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.maxsmr.core.ui"
}

dependencies {
    api("core_ui.libs:recyclerview-lib-debug-1.1.2.2@aar")
    api("core_utils.libs:commonutils-jre-1.0@jar")
    implementation(project(":core:di"))
    implementation(project(":core:android"))
    implementation(project(":designsystem:shared_res"))

    api(libs.google.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.swiperefreshlayout)

    //paging
    api(libs.androidx.paging.runtime)
    api(libs.androidx.paging.runtime.ktx)

    //Navigation
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)

    //Adapter delegates
    api(libs.hannesdorfmann.adapterdelegates4Kotlin)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
//    implementation(libs.androidx.lifecycle.extensions)

    implementation(libs.kotlinx.coroutines.android)
}