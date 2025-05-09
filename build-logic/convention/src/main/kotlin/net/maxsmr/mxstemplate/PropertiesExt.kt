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

@Suppress("UNCHECKED_CAST")
fun <V> Properties.toPropertiesMap(): Map<String, V> {
    val propertiesMap = mutableMapOf<String, V>()
    this.entries.forEach {
        val key = it.key as String? ?: return@forEach
        val value = it.value as V? ?: return@forEach
        propertiesMap[key] = value
    }
    return propertiesMap
}

fun <V> Properties.toFieldMapInfo(clazz: Class<V>): FieldMapInfo {
    val propertiesMap = toPropertiesMap<V>()

    val resultHashMap = StringBuilder("new java.util.HashMap<String, ${clazz.simpleName}>(){{ ")
    propertiesMap.forEach { (k, v) ->
        val value = if (clazz.isAssignableFrom(String::class.java)) {
            "\"${v}\""
        } else {
            "$v"
        }
        resultHashMap.append("put(\"${k}\", $value); ")
    }
    resultHashMap.append("}}")

    return FieldMapInfo(
        "java.util.Map<String, ${clazz.simpleName}>",
        resultHashMap.toString()
    )
}

data class FieldMapInfo(
    val type: String,
    val value: String,
)