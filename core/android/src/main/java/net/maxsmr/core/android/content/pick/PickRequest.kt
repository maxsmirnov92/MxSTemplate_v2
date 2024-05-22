package net.maxsmr.core.android.content.pick

import android.os.Build
import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.R
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.pick.concrete.camera.CameraPickerParams
import net.maxsmr.core.android.content.pick.concrete.media.MediaPickerParams
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams

/**
 * Запрос на взятие контента
 *
 * @param requestCode код запроса
 * @param chooserTitle заголовок плашки выбора конкретного приложения для взятия контента
 * @param errorMessage отображаемое в случае неудачи сообщение
 * @param takePhotoParams опциональные параметры для получения контента с фотокамеры
 * @param takeVideoParams опциональные параметры для полученя контента с видеокамеры
 * @param mediaParams опциональные параметры для получения расшариваемого медиаконтента (MediaStore)
 * @param safParams опциональные параметры для получения контента с помощью SAF (Storage Access Framework)
 * @param needPersistableUriAccess true, если результирующую uri планируется где-то хранить и надо
 * обеспечить долговременный доступ к ресурсу, переживающий смерть андроид-компонента, получающего этот доступ.
 * @param onSuccess действие при успешном получении контента
 * @param onError опциональное действие при ошибке получения контента, если не задано, будет показан Toast с [errorMessage]
 */
@Parcelize
class PickRequest private constructor(
    val requestCode: Int,
    val chooserTitle: TextMessage,
    val errorMessage: TextMessage,
    val takePhotoParams: CameraPickerParams?,
    val takeVideoParams: CameraPickerParams?,
    val mediaParams: MediaPickerParams?,
    val safParams: SafPickerParams?,
    val needPersistableUriAccess: Boolean,
    var onSuccess: (PickResult.Success) -> Unit,
    var onError: ((PickResult.Error) -> Unit)? = null,
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickRequest) return false

        return requestCode == other.requestCode
    }

    override fun hashCode(): Int {
        return requestCode
    }


    /**
     * Абстрактный билдер для формирования [PickRequest]. Конкретные его наследники определяются типом
     * контента для взятия и имеют public модификаторы только для подходящих под этот тип параметров.
     *
     * Например, при взятии изображения нельзя использовать [addTakeVideoParams], поэтому [BuilderImage]
     * не раскрывает метод [addTakeVideoParams].
     *
     * Обязательные для вызова методы:
     * 1. Минимум 1 из методов [addTakePhotoParams], [addTakeVideoParams], [addMediaParams], [addSafParams]
     * 1. [onSuccess]
     */
    @Suppress("unused")
    abstract class Builder protected constructor(
            private val requestCode: Int,
            contentType: ContentType,
    ) {

        private var chooserTitle: TextMessage = TextMessage(when (contentType) {
            ContentType.IMAGE -> R.string.image_picker_title
            ContentType.VIDEO -> R.string.video_picker_title
            ContentType.AUDIO -> R.string.audio_picker_title
            ContentType.DOCUMENT -> R.string.document_picker_title
        })
        private var errorMessage: TextMessage = TextMessage(when (contentType) {
            ContentType.IMAGE -> R.string.image_picker_error
            ContentType.VIDEO -> R.string.video_picker_error
            ContentType.AUDIO -> R.string.audio_picker_error
            ContentType.DOCUMENT -> R.string.document_picker_error
        })
        private var takePhotoParams: CameraPickerParams? = null
        private var takeVideoParams: CameraPickerParams? = null
        private var mediaParams: MediaPickerParams? = null
        private var safParams: SafPickerParams? = null
        private var needPersistableUriAccess: Boolean = false

        private var onPickError: ((PickResult.Error) -> Unit)? = null
        private var onPickSuccess: ((PickResult.Success) -> Unit)? = null

        fun setChooserTitle(@StringRes titleRes: Int) = apply {
            chooserTitle = TextMessage(titleRes)
        }

        fun setErrorMessage(@StringRes errorRes: Int) = apply {
            errorMessage = TextMessage(errorRes)
        }

        protected open fun addTakePhotoParams(params: CameraPickerParams) = apply {
            takePhotoParams = params
        }

        protected open fun addTakeVideoParams(params: CameraPickerParams) = apply {
            takeVideoParams = params
        }

        protected open fun addMediaParams(params: MediaPickerParams) = apply {
            if (params.contentType != ContentType.DOCUMENT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Игнорируем источник для типа DOCUMENT, если Android < Q (там нет MediaStore.Downloads таблицы)
                mediaParams = params
            }
        }

        protected open fun addSafParams(params: SafPickerParams) = apply {
            safParams = params
        }

        fun needPersistableUriAccess(needPersistableUriAccess: Boolean) = apply {
            this.needPersistableUriAccess = needPersistableUriAccess
        }

        fun onSuccess(onPickSuccess: (PickResult.Success) -> Unit) = apply {
            this.onPickSuccess = onPickSuccess
        }

        fun onError(onPickError: (PickResult.Error) -> Unit) = apply {
            this.onPickError = onPickError
        }

        fun build(): PickRequest {
            check(takePhotoParams != null || takeVideoParams != null || mediaParams != null || safParams != null) {
                "Cannot create picker without content source. Call one of addTakePhotoSource(), addTakeVideoSource(), addMediaSource(), addSafSource() methods."
            }
            check(onPickSuccess != null) {
                "Call onSuccess to specify on success action"
            }
            return PickRequest(
                    requestCode = requestCode,
                    chooserTitle = chooserTitle,
                    errorMessage = errorMessage,
                    takePhotoParams = takePhotoParams,
                    takeVideoParams = takeVideoParams,
                    mediaParams = mediaParams,
                    safParams = safParams,
                    needPersistableUriAccess = needPersistableUriAccess,
                    onSuccess = onPickSuccess!!,
                    onError = onPickError
            )
        }
    }

    /**
     * Билдер для формирования [PickRequest] контента типа [ContentType.IMAGE]
     */
    class BuilderImage(requestCode: Int) : Builder(requestCode, ContentType.IMAGE) {

        public override fun addTakePhotoParams(params: CameraPickerParams): BuilderImage {
            return super.addTakePhotoParams(params) as BuilderImage
        }

        public override fun addSafParams(params: SafPickerParams): BuilderImage {
            return super.addSafParams(params) as BuilderImage
        }

        fun addMediaParams(): BuilderImage {
            return super.addMediaParams(MediaPickerParams(ContentType.IMAGE)) as BuilderImage
        }
    }


    /**
     * Билдер для формирования [PickRequest] контента типа [ContentType.VIDEO]
     */
    class BuilderVideo(requestCode: Int) : Builder(requestCode, ContentType.VIDEO) {

        public override fun addTakeVideoParams(params: CameraPickerParams): BuilderDocument {
            return super.addTakeVideoParams(params) as BuilderDocument
        }

        public override fun addSafParams(params: SafPickerParams): BuilderVideo {
            return super.addSafParams(params) as BuilderVideo
        }

        fun addMediaParams(): BuilderImage {
            return super.addMediaParams(MediaPickerParams(ContentType.VIDEO)) as BuilderImage
        }
    }


    /**
     * Билдер для формирования [PickRequest] контента типа [ContentType.AUDIO]
     */
    class BuilderAudio(requestCode: Int) : Builder(requestCode, ContentType.AUDIO) {

        public override fun addSafParams(params: SafPickerParams): BuilderAudio {
            return super.addSafParams(params) as BuilderAudio
        }

        fun addMediaParams(): BuilderImage {
            return super.addMediaParams(MediaPickerParams(ContentType.AUDIO)) as BuilderImage
        }
    }


    /**
     * Билдер для формирования [PickRequest] контента типа [ContentType.DOCUMENT]
     */
    class BuilderDocument(requestCode: Int) : Builder(requestCode, ContentType.DOCUMENT) {

        public override fun addTakePhotoParams(params: CameraPickerParams): BuilderDocument {
            return super.addTakePhotoParams(params) as BuilderDocument
        }

        public override fun addTakeVideoParams(params: CameraPickerParams): BuilderDocument {
            return super.addTakeVideoParams(params) as BuilderDocument
        }

        public override fun addSafParams(params: SafPickerParams): BuilderDocument {
            return super.addSafParams(params) as BuilderDocument
        }
    }
}