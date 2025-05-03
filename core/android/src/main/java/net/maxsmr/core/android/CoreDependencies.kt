package net.maxsmr.core.android

import kotlinx.serialization.json.Json

lateinit var baseAppName: String
    private set

lateinit var baseJson: Json
    private set

fun initBaseAppName(appName: String) {
    if (::baseAppName.isInitialized) return
    baseAppName = appName
}

fun initBaseJson(json: Json) {
    if (::baseJson.isInitialized) return
    baseJson = json
}