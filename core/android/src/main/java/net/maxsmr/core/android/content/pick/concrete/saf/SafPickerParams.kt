package net.maxsmr.core.android.content.pick.concrete.saf

import android.content.Intent
import kotlinx.parcelize.Parcelize
import net.maxsmr.commonutils.media.MIME_TYPE_ANY
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerType

/**
 * @param intentType при подстановке пустого, type в intent будет [MIME_TYPE_ANY]
 * @param mimeTypes указывается плюсом или вместо (при основном null) в [Intent.EXTRA_MIME_TYPES]
 */
@Parcelize
class SafPickerParams @JvmOverloads constructor(
    val intentType: String? = null,
    val mimeTypes: ArrayList<String> = ArrayList(),
) : ConcretePickerParams {

    override val type: ConcretePickerType
        get() = ConcretePickerType.SAF

    override val requiredPermissions: Array<String>
        get() = emptyArray()

    companion object {

        private val DOCUMENT_MIME_TYPES = arrayListOf(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/pdf",
            "image/jpeg",
            "image/pjpeg",
            "image/png",
            "image/tiff",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/x-zip-compressed",
            "application/rar",
            "application/7z",
            "application/zip",
            "text/plain",
        )

        private val IMAGE_MIME_TYPES = arrayListOf(
            "image/jpeg",
            "image/pjpeg",
            "image/png",
        )

        @JvmStatic
        fun documents() = SafPickerParams(intentType = MIME_TYPE_ANY, mimeTypes = DOCUMENT_MIME_TYPES)

        @JvmStatic
        fun images() = SafPickerParams(intentType = MIME_TYPE_ANY, mimeTypes = IMAGE_MIME_TYPES)

        /**
         * Android не знает mime типа для .json файлов (MimeTypeMap.getSingleton().getMimeTypeFromExtension("json") == null),
         * поэтому не ограничиваем типы
         */
        @JvmStatic
        fun json() = any()

        @JvmStatic
        fun any() = SafPickerParams(intentType = MIME_TYPE_ANY)
    }
}