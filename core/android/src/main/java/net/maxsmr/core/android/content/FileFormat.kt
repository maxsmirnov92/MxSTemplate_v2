package net.maxsmr.core.android.content

/**
 * Часто используемые форматы;
 * Использовать webkit'овский метод для резолва после получения фактического "Content-Type"
 */
enum class FileFormat(
    val mimeType: String,
    val extension: String,
) {

    PDF(MIME_TYPE_PDF, "pdf"),
    XML_TEXT(MIME_TYPE_XML_TEXT, "xml"),
    XML_APPLICATION(MIME_TYPE_XML_APPLICATION, "xml"),
    TEXT(MIME_TYPE_TEXT, "txt"),
    HTML(MIME_TYPE_HTML, "html"),
    IMAGE_JPEG(MIME_TYPE_JPEG, "jpg"),
    IMAGE_PNG(MIME_TYPE_PNG, "png"),
    IMAGE_WEBP(MIME_TYPE_WEBP, "png"),
    JSON(MIME_TYPE_JSON, "json");

    companion object {

        @Deprecated("use getExtensionFromMimeType")
        fun resolveByMime(type: String): FileFormat? = entries.find { it.mimeType == type }

        @Deprecated("use getMimeTypeFromExtension")
        fun resolveByExt(ext: String): FileFormat? = entries.find { it.extension == ext }
    }
}