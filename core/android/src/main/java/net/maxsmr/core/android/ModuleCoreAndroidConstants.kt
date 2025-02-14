package net.maxsmr.core.android

import android.content.Context

/**
 * Контекст приложения.
 */
lateinit var baseApplicationContext: Context
    private set

lateinit var baseAppName: String
    private set

fun initBaseApplicationContext(context: Context) {
    if (::baseApplicationContext.isInitialized) return
    baseApplicationContext = context
}

fun initBaseAppName(appName: String) {
    if (::baseAppName.isInitialized) return
    baseAppName = appName
}

