package net.maxsmr.download.model

import net.maxsmr.commonutils.media.getExtensionFromMimeType
import net.maxsmr.commonutils.media.getMimeTypeFromName
import net.maxsmr.commonutils.media.getMimeTypeFromUrl
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.appendOrReplaceExtension
import net.maxsmr.commonutils.text.getExtension
import net.maxsmr.core.android.content.FileFormat
import java.io.Serializable

/**
 * @param resourceName имя ресурса, без расширения (при его наличии будет использовано как вспомогательное далее)
 */
open class BaseDownloadParams(
    val url: String,
    var resourceName: String,
) : Serializable {

    val resourceNameWithoutExt get() = resourceName.substringBeforeLast('.')

    /**
     * Расширение на основе исходного или обновлённого [resourceMimeType]
     */
    val extension
        get() = getExtensionFromMimeType(resourceMimeType)
            .takeIf { it.isNotEmpty() }
        // при изначально отсутствии resourceMimeTypeили если не смогли определить
        // выделять из имени
            ?: getExtension(resourceName)

    /**
     * @return целевое имя ресурса с [extension]
     */
    val targetResourceName get() = appendOrReplaceExtension(resourceName, extension)

    /**
     * опциональное MimeType ресурса, задаётся как запасной при отсутствии [HEADER_CONTENT_TYPE]
     * или форсируется его текущее значение при ignoreHeaderMimeType=true
     */
    var resourceMimeType: String = EMPTY_STRING
        set(value) {
            // могут приходить значения типа "application/pdf; charset=binary"
            val newValue = value.split(";")[0]
            field = newValue
        }

    init {
        if (resourceMimeType.isEmpty()) {
            resourceMimeType = getMimeTypeFromName(resourceName).takeIf { it.isNotEmpty() }
                ?: getMimeTypeFromUrl(url)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseDownloadParams) return false

        if (resourceName != other.resourceName) return false
        if (url != other.url) return false
        return resourceMimeType == other.resourceMimeType
    }

    override fun hashCode(): Int {
        var result = resourceName.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + resourceMimeType.hashCode()
        return result
    }

    override fun toString(): String {
        return "BaseDownloadParams(url='$url', resourceNameWithoutExt='$resourceNameWithoutExt')"
    }
}