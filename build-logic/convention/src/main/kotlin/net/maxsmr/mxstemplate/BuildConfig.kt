package net.maxsmr.mxstemplate

import org.gradle.api.JavaVersion

object BuildConfig {

    const val compileSdk = 35

    const val targetSdk = 35
    const val minSdk = 23

    val SOURCE_COMPATIBILITY_VERSION = JavaVersion.VERSION_17
}