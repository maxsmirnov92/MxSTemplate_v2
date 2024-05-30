package net.maxsmr.core.android.network

import net.maxsmr.commonutils.CHARSET_DEFAULT
import java.net.URL
import java.net.URLEncoder

@JvmOverloads
fun String?.toUrlOrNull(encoded: Boolean = true, charset: String = CHARSET_DEFAULT): URL? {
    this ?: return null
    return try {
        URL(if (!encoded) URLEncoder.encode(this, charset) else this)
    } catch (e: Exception) {
        null
    }
}

fun String?.isUrlValid(
    encoded: Boolean = true,
    charset: String = CHARSET_DEFAULT,
    orBlank: Boolean = false
) = toUrlOrNull(encoded, charset) != null || (orBlank && this.equals(URL_PAGE_BLANK, true))

const val URL_PAGE_BLANK = "about:blank"