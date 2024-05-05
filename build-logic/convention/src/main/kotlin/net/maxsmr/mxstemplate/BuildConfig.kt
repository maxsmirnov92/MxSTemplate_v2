package net.maxsmr.mxstemplate

import org.gradle.api.JavaVersion

object BuildConfig {

    const val compileSdk = 34

    const val targetSdk = 33
    const val minSdk = 24

    val SOURCE_COMPATIBILITY_VERSION = JavaVersion.VERSION_17
}