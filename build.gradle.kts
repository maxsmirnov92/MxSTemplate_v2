// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.navigation.safeargs.kotlin) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ksp) apply false
}

buildscript {

    dependencies {
        // Тут остаются classpath, для которых пока нет соотв. плагина
        classpath(libs.android.gradle)
        classpath(libs.androidx.navigation.plugin)
        classpath(libs.huawei.agconnect.agcp)
        classpath(libs.kotlin.compose)
    }
}