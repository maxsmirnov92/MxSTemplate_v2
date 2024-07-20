package net.maxsmr.feature.download.data.model

import net.maxsmr.commonutils.media.getExtensionFromMimeType
import net.maxsmr.commonutils.media.getMimeTypeFromName
import net.maxsmr.commonutils.media.getMimeTypeFromUrl
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.commonutils.text.getExtension
import net.maxsmr.commonutils.text.removeExtension
import java.io.Serializable

/**
 * @param resourceName имя ресурса, без расширения (при его наличии будет использовано как вспомогательное далее)
 */
open class BaseDownloadParams(
    val url: String,
    var resourceName: String,
    private val withExtFromContentType: Boolean = true
) : Serializable {

    val resourceNameWithoutExt get() = resourceName.removeExtension()

    /**
     * Расширение на основе исходного или обновлённого [resourceMimeType]
     */
    val extension: String
        get() {
            val extFromName = resourceName.getExtension()
            val extFromType = if (withExtFromContentType || extFromName.isEmpty()) {
                getExtensionFromMimeType(resourceMimeType).takeIf { it.isNotEmpty() }
            } else {
                null
            }
            // при изначальном отсутствии resourceMimeType
            // или если не смогли определить оттуда
            // или надо принудительно из имени
            return extFromType ?: extFromName
        }

    /**
     * @return целевое имя ресурса с [extension]
     */
    val targetResourceName get() = resourceName.appendExtension(extension)

    /**
     * опциональное MimeType ресурса, задаётся как запасной при отсутствии [HEADER_CONTENT_TYPE]
     * или форсируется его текущее значение при withExtFromContentType=true
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
        if (resourceMimeType != other.resourceMimeType) return false
        return withExtFromContentType == other.withExtFromContentType
    }

    override fun hashCode(): Int {
        var result = resourceName.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + resourceMimeType.hashCode()
        result = 31 * result + withExtFromContentType.hashCode()
        return result
    }

    override fun toString(): String {
        return "BaseDownloadParams(url='$url', resourceNameWithoutExt='$resourceNameWithoutExt', ignoreContentType='$withExtFromContentType')"
    }
}