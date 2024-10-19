package net.maxsmr.feature.camera.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.CameraState
import androidx.camera.core.ImageProxy
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
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine.Companion.joinLines
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.camera.CameraFacing
import net.maxsmr.feature.camera.FrameCalculator
import net.maxsmr.feature.camera.R
import net.maxsmr.feature.camera.recognition.ITextRecognition
import java.util.concurrent.Executor

class CameraXRecognitionViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted val imageAnalyzerExecutor: Executor,
    @Assisted private val numberLength: Int,
    private val textRecognition: ITextRecognition,
) : BaseHandleableViewModel(state) {

    init {
        require(numberLength > 0) {"Incorrect numberLength: $numberLength"}
    }

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

    /**
     * Текущее состояние распознавания
     */
    val recognitionStateLiveData by persistableLiveDataInitial(false)

    val frameStatsLiveData by persistableLiveData<FrameCalculator.FrameStats?>()

    val realtimeResultsLiveData by persistableLiveData<NumberRecognitionResult?>()

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
            if (it is TextRecognitionResult.RecognizedText) {
                showOkDialog(
                    DIALOG_TAG_CAPTURE_RECOGNITION_RESULT,
                    TextMessage(it.text),
                    TextMessage(R.string.camera_dialog_capture_recognition_result_title)
                )
            } else if (it is TextRecognitionResult.FailedRecognition) {
                showOkDialog(
                    DIALOG_TAG_CAPTURE_RECOGNITION_RESULT,
                    TextMessage(R.string.camera_dialog_capture_recognition_result_title),
                    TextMessage(R.string.camera_dialog_capture_recognition_result_message_failed_format,
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
        frameCalculator.onFrame()

        if (recognitionStateLiveData.value != true) {
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
            try {
                val result = textRecognition.processFrame(frame, rotationDegrees)
                val number = result.map { it.text }.firstOrNull {
                    val subNumbers = it.split(" ")
                    subNumbers.isNotEmpty() && subNumbers.flatMap { it.asIterable() }.all { it.isDigit() }
                }
                val lines = result.joinLines()
                realtimeResultsLiveData.postValue(
                    if (number != null && number.replace(" ", "").length >= numberLength) {
                        NumberRecognitionResult.RecognizedNumber(number, lines)
                    } else {
                        NumberRecognitionResult.FailedRecognition(lines, null)
                    }
                )
            } catch (e: Exception) {
                logException(logger, e, "processFrame")
                realtimeResultsLiveData.postValue(NumberRecognitionResult.FailedRecognition(null, e))
            } finally {
                imageProxy.close()
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
                captureResultsLiveData.postValue(TextRecognitionResult.RecognizedText(result.joinLines()))
            } catch (e: Exception) {
                logException(logger, e, "processFrame")
                captureResultsLiveData.postValue(TextRecognitionResult.FailedRecognition(e))
            } finally {
                imageBitmap.recycle()
            }
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

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            imageAnalyzerExecutor: Executor,
            numberLength: Int = 16 // TODO разные стратегии определения по регуляркам
        ): CameraXRecognitionViewModel
    }

    sealed interface NumberRecognitionResult {

        data class RecognizedNumber(val number: String, val text: String) : NumberRecognitionResult

        data class FailedRecognition(val text: String?, val exception: Exception?) : NumberRecognitionResult
    }

    sealed interface TextRecognitionResult {

        data class RecognizedText(val text: String) : TextRecognitionResult

        data class FailedRecognition(val exception: Exception) : TextRecognitionResult
    }

    companion object {

        const val DIALOG_TAG_CAPTURE_RECOGNITION_RESULT = "capture_recognition_result"

        private const val KEY_FIELD_CAMERA_FACING = "camera_facing"
    }
}