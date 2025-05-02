package net.maxsmr.feature.camera.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.commonutils.graphic.createBitmapFromUri
import net.maxsmr.commonutils.graphic.isBitmapValid
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.message.errorMessage
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.states.ILoadState
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.delegates.persistableStateFlow
import net.maxsmr.core.android.content.storage.ContentStorage.StorageType
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.data
import net.maxsmr.core.android.coroutines.execute.succeeded
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine.Companion.joinLines
import net.maxsmr.core.ui.field.createField
import net.maxsmr.feature.camera.CameraFacing
import net.maxsmr.feature.camera.CameraXController
import net.maxsmr.feature.camera.CameraXController.ErrorCallbacks
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
    @ApplicationContext private val context: Context,
) : BaseViewModel(state, context), ErrorCallbacks {

    /**
     * Целевой тип камеры (совпадёт с фактическим при успешном подключении)
     */
    val cameraFacingField: Field<CameraFacing?> = createField(
        initialValue = CameraFacing.BACK,
        key = KEY_FIELD_CAMERA_FACING
    ) {
        emptyIf { it == null }
    }

    /**
     * Текущее состояние подсветки
     */
    val flashLightStateFlow: StateFlow<Boolean?> by lazy {
        _flashLightStateFlow.asStateFlow()
    }

    val frameStatsStateFlow: StateFlow<FrameCalculator.FrameStats?> by lazy {
        _frameStatsStateFlow.asStateFlow()
    }

    val realtimeResultsStateFlow: StateFlow<TextRecognitionResult?> by lazy {
        _realtimeResultsStateFlow.asStateFlow()
    }

    val captureResultsStateFlow: StateFlow<TextRecognitionResult?> by lazy {
        _captureResultsStateFlow.asStateFlow()
    }

    /**
     * Текущее состояние распознавания
     */
    val recognitionStateFlow: StateFlow<Boolean> by lazy {
        _recognitionStateFlow.asStateFlow()
    }

    private val _flashLightStateFlow by persistableStateFlow<Boolean?>(null)

    private val _frameStatsStateFlow by persistableStateFlow<FrameCalculator.FrameStats?>(null)

    private val _realtimeResultsStateFlow by persistableStateFlow<TextRecognitionResult?>(null)

    private val _captureResultsStateFlow by persistableStateFlow<TextRecognitionResult?>(null)

    private val _recognitionStateFlow by persistableStateFlow(false)

    private val dispatcher: CoroutineDispatcher by lazy {
        imageAnalyzerExecutor.asCoroutineDispatcher()
    }

    private val frameCalculator: FrameCalculator by lazy {
        FrameCalculator { stats, _ ->
            _frameStatsStateFlow.value = stats
        }
    }

    private var jobs: CameraObservableJobs? = null

    private var cameraStateCollectJob: Job? = null

    override fun onInitialized() {
        super.onInitialized()
        _captureResultsStateFlow.observe {
            if (it is TextRecognitionResult.Success) {
                showOkDialog(
                    DIALOG_TAG_CAPTURE_RECOGNITION_RESULT,
                    it.message,
                    TextMessage(R.string.camera_dialog_capture_recognition_result_title),
                    configBlock = {
                        setOnClose {
                            _captureResultsStateFlow.value = null
                        }
                    }
                )
            } else if (it is TextRecognitionResult.Failed) {
                showOkDialog(
                    DIALOG_TAG_CAPTURE_RECOGNITION_RESULT,
                    TextMessage(R.string.camera_dialog_capture_recognition_result_title),
                    TextMessage(
                        R.string.camera_recognize_text_failed_format,
                        it.exception.message
                    ),
                    configBlock = {
                        setOnClose {
                            _captureResultsStateFlow.value = null
                        }
                    }
                )
            }
        }
    }

    override fun onCameraStartError(e: Exception) {
        showCameraOpenError(e)
    }

    override fun onCameraStateError(e: CameraState.StateError) {
        showCameraStateError(e)
    }

    override fun onCleared() {
        super.onCleared()
        clearStatsData()
        textRecognition.dispose()
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun onFrameReceived(imageProxy: ImageProxy) {
        frameCalculator.onFrame() // imageProxy.imageInfo.timestamp

        if (!_recognitionStateFlow.value) {
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
                _realtimeResultsStateFlow.value = TextRecognitionResult.Failed(e)
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
                _realtimeResultsStateFlow.value = result
            }
        }
    }

    fun observeController(controller: CameraXController) {
        cameraStateCollectJob?.cancel()
        cameraStateCollectJob = controller.cameraStateType.observe {

            fun cancel() {
                jobs?.let {
                    it.torchCollectJob.cancel()
//                it.zoomState.cancel()
                    jobs = null
                }
            }

            val isOpened = controller.isCameraOpened
            if (!isOpened) {
                _flashLightStateFlow.value = null
                clearStatsData()
            }
//            if (isOpened && controller.cameraInfo?.hasFlashUnit() != true) {
//                flashLightState.value = null
//            }

            controller.observables?.let {
                if (isOpened) {
                    cancel()
                    jobs = CameraObservableJobs(it.torchState.observe { state ->
                        _flashLightStateFlow.value = when (state) {
                            CameraXController.TorchState.ON -> true
                            CameraXController.TorchState.OFF -> false
                            else -> null
                        }
                    }
//                        it.zoomState.observe() {}
                    )
                }

                if (controller.isCameraClosed) {
                    cancel()
                }
            }
        }
    }

    fun toggleRecognitionState() {
        val state = recognitionStateFlow.value
        setRecognitionState(!state)
    }

    fun takePicture(
        controller: CameraXController,
        storageType: StorageType,
        resourceNameFunc: (Long) -> String = { it.toString() },
        imageConfig: (ImageCapture.() -> Unit)? = null,
    ): Flow<LoadState<Uri?>> {
        return controller.takePicture(
            storageType,
            resourceNameFunc,
            imageConfig
        ).also { flow ->
            flow.observe {
                if (!it.isLoading) {
                    val data = it.getData()
                    if (data != null) {
                        createBitmapFromUri(data, context.contentResolver)?.let { bitmap ->
                            onImageCaptured(bitmap)
                        }
                    } else {
                        it.error?.let { e ->
                            showTakePictureError(e)
                        }
                    }
                }
            }
        }
    }

    private fun onImageCaptured(imageBitmap: Bitmap, rotationDegrees: Int = 90) {
        // TODO в UseCase
        if (!isBitmapValid(imageBitmap)) {
            return
        }
        viewModelScope.launch(dispatcher) {
            try {
                val result = textRecognition.processCapture(imageBitmap, rotationDegrees)
                if (result.isEmpty()) {
                    throw EmptyResultException()
                }
                _captureResultsStateFlow.value = TextRecognitionResult.Success(TextMessage(result.joinLines()))
            } catch (e: Exception) {
                logException(logger, e, "processFrame")
                _captureResultsStateFlow.value = TextRecognitionResult.Failed(e)
            } finally {
                imageBitmap.recycle()
            }
        }
    }

    private fun setRecognitionState(toggle: Boolean) {
        if (!toggle || textMatcherUseCases.isNotEmpty()) {
            _recognitionStateFlow.value = toggle
        }
    }

    private fun showCameraOpenError(e: Throwable) {
        showSnackbar(
            TextMessage(
                R.string.camera_error_open_format,
                e.message.takeIf { !it.isNullOrEmpty() } ?: e.toString()))
    }

    private fun showCameraStateError(e: CameraState.StateError) {
        showSnackbar(TextMessage(R.string.camera_error_state_format, "${e.code} ${e.type}"))
    }

    private fun showTakePictureError(e: ILoadState.ErrorData) {
        showSnackbar(
            TextMessage(
                R.string.camera_error_take_picture_format,
                e.errorMessage() ?: e.toString()
            )
        )
    }

    private fun clearStatsData() {
        frameCalculator.onStop()
        _frameStatsStateFlow.value = null
        _realtimeResultsStateFlow.value = null
        _captureResultsStateFlow.value = null
    }

    private suspend fun BaseTextMatcherUseCase<*>.invokeWithLines(lines: List<RecognizedLine>): TextRecognitionResult {
        val result = this.invoke(lines)
        return if (result.succeeded) {
            when (val r = result.data?.result) {
                is DocTypeTextMatcherUseCase.DocumentResult -> {
                    TextRecognitionResult.Success(
                        TextMessage(
                            R.string.camera_recognize_text_type_doc_type_format,
                            r.number,
                            r.type.name
                        )
                    )
                }

                is GrzTextMatcherUseCase.GrzResult -> {
                    TextRecognitionResult.Success(
                        TextMessage(
                            R.string.camera_recognize_text_type_grz_format,
                            r.number,
                            r.type.name
                        )
                    )
                }

                is BankCardTextMatcherNumberUseCase.CardNumberResult -> {
                    TextRecognitionResult.Success(
                        TextMessage(
                            R.string.camera_recognize_text_type_bank_card_format,
                            result.data?.sourceText
                        )
                    )
                }

                is EmailTextMatcherUseCase.EmailResult -> {
                    TextRecognitionResult.Success(
                        TextMessage(
                            R.string.camera_recognize_text_type_email_format,
                            r.email
                        )
                    )
                }

                is RusPhoneTextMatcherUseCase.RusPhoneResult -> {
                    TextRecognitionResult.Success(
                        TextMessage(
                            R.string.camera_recognize_text_type_rus_phone_format,
                            r.phone
                        )
                    )
                }

                is String -> {
                    TextRecognitionResult.Success(TextMessage(r))
                }

                else -> {
                    TextRecognitionResult.Failed(RuntimeException("Unknown result type: $r"))
                }
            }
        } else if (result is ExecuteResult.Error) {
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

    sealed interface TextRecognitionResult : Serializable {

        data class Success(val message: TextMessage) : TextRecognitionResult

        data class Failed(val exception: Throwable, val sourceText: String? = null) : TextRecognitionResult
    }

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            imageAnalyzerExecutor: Executor,
            textRecognitionUseCases: List<BaseTextMatcherUseCase<*>>,
        ): CameraXRecognitionViewModel
    }

    private class CameraObservableJobs(
        val torchCollectJob: Job,
//        val zoomCollectJob: Job,
    )

    companion object {

        const val DIALOG_TAG_CAPTURE_RECOGNITION_RESULT = "capture_recognition_result"

        private const val KEY_FIELD_CAMERA_FACING = "camera_facing"
    }
}