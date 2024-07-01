package net.maxsmr.core.android.network

import android.content.ContentResolver
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
): Boolean {
    if (orBlank && this.equals(URL_PAGE_BLANK, true)) {
        return true
    }
    val uri = Uri.parse(if (!encoded) URLEncoder.encode(this, charset) else this)
    if (uri.scheme.isAnyResourceScheme()) {
        return false
    }
    return uri.host?.contains('.') == true
}

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

fun String?.isAnyResourceScheme() = !this.isNullOrEmpty() && RESOURCE_SCHEMES.any { this.equals(it, true) }

fun String?.isAnyNetScheme() = !this.isNullOrEmpty() && NET_SCHEMES.any { this.equals(it, true) }

const val URL_PAGE_BLANK = "about:blank"

const val SCHEME_HTTPS = "https"
const val SCHEME_HTTP = "http"

private val RESOURCE_SCHEMES = listOf(ContentResolver.SCHEME_CONTENT,
    ContentResolver.SCHEME_FILE,
    ContentResolver.SCHEME_ANDROID_RESOURCE)

private val NET_SCHEMES = listOf(SCHEME_HTTPS, SCHEME_HTTP, "about", "javascript")