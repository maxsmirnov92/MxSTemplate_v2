plugins {
    alias(libs.plugins.mxs.template.library)
    alias(libs.plugins.mxs.template.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.maxsmr.core.ui.view"
}

dependencies {
    api("core_ui_base.libs:recyclerview-lib-release-1.1.2.2@aar")

    implementation(project(":core:ui:base"))
    implementation(project(":core:android"))

    api(libs.google.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.swiperefreshlayout)

    //Adapter delegates
    api(libs.hannesdorfmann.adapterdelegates4Kotlin)
}