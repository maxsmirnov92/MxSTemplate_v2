package net.maxsmr.core.android.content

/**
 * Часто используемые форматы;
 * Использовать webkit'овский метод для резолва после получения фактического "Content-Type"
 */
enum class FileFormat(
    val mimeType: String,
    val extension: String,
) {

    PDF("application/pdf", "pdf"),
    XML_TEXT("text/xml", "xml"),
    XML_APPLICATION("application/xml", "xml"),
    TEXT("text/plain", "txt"),
    HTML("text/html", "html"),
    IMAGE_JPEG("image/jpeg", "jpg"),
    IMAGE_PNG("image/png", "png"),
    IMAGE_WEBP("image/webp", "png"),
    JSON("application/json", "json");

    companion object {

        @Deprecated("use getExtensionFromMimeType")
        fun resolveByMime(type: String): FileFormat? = entries.find { it.mimeType == type }

        @Deprecated("use getMimeTypeFromExtension")
        fun resolveByExt(ext: String): FileFormat? = entries.find { it.extension == ext }
    }
}