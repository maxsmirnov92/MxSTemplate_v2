package net.maxsmr.core.android.network

import android.net.Uri
import net.maxsmr.commonutils.CHARSET_DEFAULT
import okhttp3.internal.publicsuffix.PublicSuffixDatabase
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
    orBlank: Boolean = false,
) = toUrlOrNull(encoded, charset) != null || (orBlank && this.equals(URL_PAGE_BLANK, true))

fun String?.equalsIgnoreSubDomain(other: String?): Boolean {
    val thisUri = this?.let { Uri.parse(it) }
    val otherUri = other?.let { Uri.parse(it) }
    return thisUri.equalsIgnoreSubDomain(otherUri)
}

fun Uri?.equalsIgnoreSubDomain(other: Uri?): Boolean {
    if (this == null && other == null) {
        return true
    }
    fun Uri.excludeSubDomain(): String =
        PublicSuffixDatabase.get().getEffectiveTldPlusOne(this.host.orEmpty()).orEmpty()
    return if (this != null) {
        val thisDomain = this.excludeSubDomain()
        val otherDomain = other?.excludeSubDomain().orEmpty()
        thisDomain.equals(otherDomain, true)
    } else {
        false
    }
}

const val URL_PAGE_BLANK = "about:blank"