package net.maxsmr.core.android.content.pick.concrete.camera

import android.Manifest
import android.provider.MediaStore
import kotlinx.parcelize.Parcelize
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerType
import net.maxsmr.core.android.content.storage.ContentStorage

/**
 * Параметры для взятия фото/видео с камеры
 *
 * @param namePrefix имя файла без расширения, в который будет помещен результат
 * @param pickType тип параметров, фото или видео
 * @param storageType определяет область памяти, куда поместить результат
 * @param unique true, если к имени файла надо добавить уникальный постфикс, иначе false
 * @param extension расширение файла, начинающееся с "."
 */
@Parcelize
class CameraPickerParams internal constructor(
    val namePrefix: String,
    val pickType: PickType,
    val storageType: ContentStorage.StorageType,
    val unique: Boolean = true,
    val extension: String = EMPTY_STRING,
    val subPath: String = EMPTY_STRING,
) : ConcretePickerParams {

    init {
        check(unique || namePrefix.isNotBlank()) {
            "Param \'namePrefix\' mustn't be blank for non unique files"
        }
    }

    override val type: ConcretePickerType
        get() = when (pickType) {
            PickType.PHOTO -> ConcretePickerType.PHOTO
            PickType.VIDEO -> ConcretePickerType.VIDEO
        }

    override val requiredPermissions: Array<String>
        get() = arrayOf(Manifest.permission.CAMERA)


    /**
     * Тип парамеров
     */
    enum class PickType(val intentAction: String) {

        /**
         * Взятие фото с камеры
         */
        PHOTO(MediaStore.ACTION_IMAGE_CAPTURE) {

            override fun toContentType(): ContentType = ContentType.IMAGE
        },

        /**
         * Взятие видео с камеры
         */
        VIDEO(MediaStore.ACTION_VIDEO_CAPTURE) {

            override fun toContentType(): ContentType = ContentType.VIDEO
        };

        abstract fun toContentType(): ContentType
    }


    companion object {

        @JvmStatic
        @JvmOverloads
        fun photo(
            name: String,
            storageType: ContentStorage.StorageType,
            unique: Boolean = true,
            extension: String = ".jpg",
            subPath: String = EMPTY_STRING,
        ) = CameraPickerParams(
            name,
            PickType.PHOTO,
            storageType,
            unique,
            extension,
            subPath
        )

        @JvmStatic
        @JvmOverloads
        fun video(
            name: String,
            storageType: ContentStorage.StorageType,
            unique: Boolean = true,
            extension: String = ".mp4",
            subPath: String = EMPTY_STRING,
        ) = CameraPickerParams(
            name,
            PickType.VIDEO,
            storageType,
            unique,
            extension,
            subPath
        )
    }
}