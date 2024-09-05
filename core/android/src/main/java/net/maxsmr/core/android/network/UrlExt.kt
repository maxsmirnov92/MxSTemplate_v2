package net.maxsmr.core.android.network

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import net.maxsmr.commonutils.text.EMPTY_STRING
import okhttp3.internal.publicsuffix.PublicSuffixDatabase
import java.net.URL

/**
 * Использовать обычный [toUri], если нужны только не http/https схемы
 */
fun String?.toValidUri(
    orBlank: Boolean = false,
    isNonResource: Boolean = true,
    schemeIfEmpty: String? = null,
): Uri? {
    if (this == null) {
        return null
    }
    if (orBlank && this.equals(URL_PAGE_BLANK, true)) {
        return toUri()
    } else {
        var uri = this.toUri()

        if (!schemeIfEmpty.isNullOrEmpty() && uri.scheme.isNullOrEmpty()) {
            // у урлы нет схемы - подставляем непустой префикс и получаем урлу с возможным хостом
            val uriString = uri.toString()
            uri = "$schemeIfEmpty://$uriString".toUri()
        }

        if (uri.scheme.isAnyResourceScheme()) {
            return if (isNonResource) {
                // ресурсные схемы под запретом
                null
            } else {
                // хост в ресурсной схеме не проверяем
                uri
            }
        }
        return uri.takeIf { it.host.isHostValid() }
    }
}

fun String?.toUrlOrNull(): URL? {
    this ?: return null
    return try {
        URL(this)
    } catch (e: Exception) {
        null
    }
}

fun String?.isUrlValid(
    orBlank: Boolean = false,
    isNonResource: Boolean = true,
    schemeIfEmpty: String? = null,
): Boolean {
    return toValidUri(orBlank, isNonResource, schemeIfEmpty) != null
}

fun String?.isHostValid() = getHostParts().size > 1

/**
 * Сравнение двух хостов без учёта первого домена
 */
fun String?.equalsIgnoreSubDomain(other: String?): Boolean {
    fun String?.excludeSubDomain(): String {
        return if (this != null && this.isHostValid()) {
            // выкидываем часть только если это "www"
            if ("www".equals(getHostParts().getOrNull(0), true)) {
                PublicSuffixDatabase.get().getEffectiveTldPlusOne(this).orEmpty()
            } else {
                this
            }
        } else {
            EMPTY_STRING
        }
    }

    val thisDomain = this.excludeSubDomain()
    val otherDomain = other?.excludeSubDomain().orEmpty()
    return thisDomain.equals(otherDomain, true)
}

fun Uri?.equalsIgnoreSubDomain(other: Uri?): Boolean {
    if (this == null && other == null) {
        return true
    }
    return this?.host?.equalsIgnoreSubDomain(other?.host) ?: false
}

fun String?.isAnyResourceScheme() = !this.isNullOrEmpty() && RESOURCE_SCHEMES.any { this.equals(it, true) }

fun String?.isAnyNetScheme() = !this.isNullOrEmpty() && NET_SCHEMES.any { this.equals(it, true) }

private fun String?.getHostParts(): List<String> = this?.split(".")
    ?.takeIf { parts ->
        parts.all { it.isNotEmpty() }
    }.orEmpty()

const val URL_PAGE_BLANK = "about:blank"

const val URL_SCHEME_HTTPS = "https"
const val URL_SCHEME_HTTP = "http"

private val RESOURCE_SCHEMES = listOf(
    ContentResolver.SCHEME_CONTENT,
    ContentResolver.SCHEME_FILE,
    ContentResolver.SCHEME_ANDROID_RESOURCE
)

private val NET_SCHEMES = listOf(URL_SCHEME_HTTPS, URL_SCHEME_HTTP, "about", "javascript")