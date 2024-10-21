package net.maxsmr.feature.camera.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.CameraState
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.graphic.isBitmapValid
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.android.coroutines.usecase.data
import net.maxsmr.core.android.coroutines.usecase.succeeded
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine.Companion.joinLines
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.camera.CameraFacing
import net.maxsmr.feature.camera.FrameCalculator
import net.maxsmr.feature.camera.R
import net.maxsmr.feature.camera.recognition.ITextRecognition
import net.maxsmr.feature.camera.recognition.cases.BankCardTextMatcherNumberUseCase
import net.maxsmr.feature.camera.recognition.cases.BaseTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.DocTypeTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.EmailTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.GrzTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.RusPhoneTextMatcherUseCase
import java.io.Serializable
import java.util.concurrent.Executor

/**
 * @param textMatcherUseCases юзкейсы для применения в onFrameReceived
 * @param textRecognition Google или Huawei реализация распознавателя текста
 */
class CameraXRecognitionViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted val imageAnalyzerExecutor: Executor,
    @Assisted val textMatcherUseCases: List<BaseTextMatcherUseCase<*>>,
    private val textRecognition: ITextRecognition,
) : BaseHandleableViewModel(state) {

    /**
     * Целевой тип камеры (совпадёт с фактическим при успешном подключении)
     */
    val cameraFacingField: Field<CameraFacing?> = Field.Builder<CameraFacing?>(CameraFacing.BACK)
        .emptyIf { false }
        .persist(state, KEY_FIELD_CAMERA_FACING)
        .build()

    /**
     * Текущее состояние подсветки
     */
    val flashLightStateLiveData by persistableLiveDataInitial<Boolean?>(null)

    private val _recognitionStateLiveData by persistableLiveDataInitial(false)

    /**
     * Текущее состояние распознавания
     */
    val recognitionStateLiveData = _recognitionStateLiveData as LiveData<Boolean>

    val frameStatsLiveData by persistableLiveData<FrameCalculator.FrameStats?>()

    val realtimeResultsLiveData by persistableLiveData<TextRecognitionResult?>()

    val captureResultsLiveData by persistableLiveData<TextRecognitionResult?>()

    private val dispatcher: CoroutineDispatcher by lazy {
        imageAnalyzerExecutor.asCoroutineDispatcher()
    }

    private val frameCalculator: FrameCalculator by lazy {
        FrameCalculator { stats, _ ->
            frameStatsLiveData.value = stats
        }
    }

    override fun onInitialized() {
        super.onInitialized()
        captureResultsLiveData.observe {
            if (it is TextRecognitionResult.Success) {
                showOkDialog(
                    DIALOG_TAG_CAPTURE_RECOGNITION_RESULT,
                    it.message,
                    TextMessage(R.string.camera_dialog_capture_recognition_result_title)
                )
            } else if (it is TextRecognitionResult.Failed) {
                showOkDialog(
                    DIALOG_TAG_CAPTURE_RECOGNITION_RESULT,
                    TextMessage(R.string.camera_dialog_capture_recognition_result_title),
                    TextMessage(R.string.camera_recognize_text_failed_format,
                        it.exception.message)
                )
            }
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(delegate)
        delegate.bindAlertDialog(DIALOG_TAG_CAPTURE_RECOGNITION_RESULT) {
            it.asOkDialog(delegate.context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearStatsData()
        textRecognition.dispose()
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun onFrameReceived(imageProxy: ImageProxy) {
        frameCalculator.onFrame() // imageProxy.imageInfo.timestamp

        if (_recognitionStateLiveData.value != true) {
            imageProxy.close()
            return
        }

        val frame = imageProxy.image
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        if (frame == null) {
            imageProxy.close()
            return
        }

        viewModelScope.launch(dispatcher) {

            val recognizedLines = try {
               textRecognition.processFrame(frame, rotationDegrees)
            } catch (e: Exception) {
                logException(logger, e, "processFrame")
                realtimeResultsLiveData.postValue(TextRecognitionResult.Failed(e))
                null
            } finally {
                imageProxy.close()
            }

            recognizedLines?.let {
                val results = textMatcherUseCases.map {
                    it.invokeWithLines(recognizedLines)
                }
                val result = results.find {
                    it is TextRecognitionResult.Success
                } ?: results.find {
                    it is TextRecognitionResult.Failed
                }
                realtimeResultsLiveData.postValue(result)
            }
        }
    }

    fun onImageCaptured(imageBitmap: Bitmap, rotationDegrees: Int = 90) {
        if (!isBitmapValid(imageBitmap)) {
            return
        }
        viewModelScope.launch(dispatcher) {
            try {
                val result = textRecognition.processCapture(imageBitmap, rotationDegrees)
                if (result.isEmpty()) {
                    throw EmptyResultException()
                }
                captureResultsLiveData.postValue(TextRecognitionResult.Success(TextMessage(result.joinLines())))
            } catch (e: Exception) {
                logException(logger, e, "processFrame")
                captureResultsLiveData.postValue(TextRecognitionResult.Failed(e))
            } finally {
                imageBitmap.recycle()
            }
        }
    }

    fun setRecognitionState(toggle: Boolean) {
        if (!toggle || textMatcherUseCases.isNotEmpty()) {
            _recognitionStateLiveData.value = toggle
        }
    }

    fun clearStatsData() {
        frameCalculator.onStop()
        frameStatsLiveData.value = null
        realtimeResultsLiveData.value = null
        captureResultsLiveData.value = null
    }

    fun showCameraOpenError(e: Throwable) {
        showSnackbar(
            TextMessage(
                R.string.camera_error_open_format,
                e.message.takeIf { !it.isNullOrEmpty() } ?: e.toString()))
    }

    fun showCameraStateError(e: CameraState.StateError) {
        showSnackbar(TextMessage(R.string.camera_error_state_format, "${e.code} ${e.type}"))
    }

    fun showTakePictureError(e: Throwable) {
        showSnackbar(
            TextMessage(
                R.string.camera_error_take_picture_format,
                e.message.takeIf { !it.isNullOrEmpty() } ?: e.toString()))
    }

    private suspend fun BaseTextMatcherUseCase<*>.invokeWithLines(lines: List<RecognizedLine>): TextRecognitionResult {
        val result = this.invoke(lines)
        return if (result.succeeded) {
            when(val r = result.data?.result) {
                is DocTypeTextMatcherUseCase.DocumentResult -> {
                    TextRecognitionResult.Success(TextMessage(R.string.camera_recognize_text_type_doc_type_format, r.number, r.type.name))
                }
                is GrzTextMatcherUseCase.GrzResult -> {
                    TextRecognitionResult.Success(TextMessage(R.string.camera_recognize_text_type_grz_format, r.number, r.type.name))
                }
                is BankCardTextMatcherNumberUseCase.CardNumberResult -> {
                    TextRecognitionResult.Success(TextMessage(R.string.camera_recognize_text_type_bank_card_format, result.data?.sourceText))
                }
                is EmailTextMatcherUseCase.EmailResult -> {
                    TextRecognitionResult.Success(TextMessage(R.string.camera_recognize_text_type_email_format, r.email))
                }
                is RusPhoneTextMatcherUseCase.RusPhoneResult -> {
                    TextRecognitionResult.Success(TextMessage(R.string.camera_recognize_text_type_rus_phone_format, r.phone))
                }
                is String -> {
                    TextRecognitionResult.Success(TextMessage(r))
                }
                else -> {
                    TextRecognitionResult.Failed(RuntimeException("Unknown result type: $r"))
                }
            }
        } else if (result is UseCaseResult.Error) {
            val e = result.exception
            if (e is BaseTextMatcherUseCase.FailedRecognitionException) {
                TextRecognitionResult.Failed(e, e.sourceText)
            } else {
                TextRecognitionResult.Failed(e)
            }
        } else {
            TextRecognitionResult.Failed(RuntimeException())
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            imageAnalyzerExecutor: Executor,
            textRecognitionUseCases: List<BaseTextMatcherUseCase<*>>
        ): CameraXRecognitionViewModel
    }

    sealed interface TextRecognitionResult: Serializable {

        data class Success(val message: TextMessage) : TextRecognitionResult

        data class Failed(val exception: Throwable, val sourceText: String? = null) : TextRecognitionResult
    }

    companion object {

        const val DIALOG_TAG_CAPTURE_RECOGNITION_RESULT = "capture_recognition_result"

        private const val KEY_FIELD_CAMERA_FACING = "camera_facing"
    }
}