package net.maxsmr.core.android

import android.content.Context

const val PLATFORM_NAME = "Android"

internal const val VERSION_NOT_SET = -1

/**
 * Контекст приложения.
 */
lateinit var baseApplicationContext: Context
    private set

lateinit var baseAppName: String
    private set

fun initModuleCoreAndroidContext(context: Context) {
    baseApplicationContext = context
}

fun initBaseAppName(appName: String) {
    baseAppName = appName
}


