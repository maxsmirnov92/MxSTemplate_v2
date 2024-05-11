package net.maxsmr.core.android.network

import java.net.URL
import java.net.URLEncoder

@JvmOverloads
fun String?.toUrlOrNull(encoded: Boolean = true, charset: String = "UTF-8"): URL? {
    this ?: return null
    return try {
        URL(if (!encoded) URLEncoder.encode(this, charset) else this)
    } catch (e: Exception) {
        null
    }
}