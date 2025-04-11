package net.maxsmr.core.android

lateinit var baseAppName: String
    private set

fun initBaseAppName(appName: String) {
    if (::baseAppName.isInitialized) return
    baseAppName = appName
}

