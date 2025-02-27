package net.maxsmr.mxstemplate

import java.io.File
import java.util.Properties

fun File.loadProperties(): Properties {
    val properties = Properties()
    if (exists() && isFile) {
        properties.load(inputStream())
    }
    return properties
}

fun Properties.getPropertyNotNull(key: String, fallback: String = ""): String =
    getProperty(key)?.takeIf { it != "null" } ?: fallback

fun Properties.getStringPropertyNotNull(key: String, fallback: String = ""): String =
    "\"${getPropertyNotNull(key, fallback)}\""