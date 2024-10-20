package net.maxsmr.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import net.maxsmr.commonutils.asActivityOrThrow
import net.maxsmr.commonutils.gui.DiffOrientationEventListener
import net.maxsmr.commonutils.gui.StandardAspectRatio
import net.maxsmr.commonutils.gui.getDisplayStandardAspectRatio
import net.maxsmr.commonutils.gui.getSurfaceRotation
import net.maxsmr.commonutils.lifecycleOwnerOrThrow
import net.maxsmr.commonutils.live.errorLoad
import net.maxsmr.commonutils.live.just
import net.maxsmr.commonutils.live.loading
import net.maxsmr.commonutils.live.successLoad
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.media.delete
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.ContentStorage.StorageType
import net.maxsmr.feature.camera.utils.getCorrectedRotationDegreesForCameraX
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * @param imageAnalyzerProvider null, если анализ фреймов не требуется
 */
class CameraXController(
    private val previewView: PreviewView,
    private val imageAnalyzerExecutor: Executor,
    private val lensFacingProvider: () -> CameraFacing = { CameraFacing.BACK },
    private val imageAnalyzerProvider: (() -> ImageAnalysis.Analyzer)? = null,
    private val previewBuilderConfig: (Preview.Builder.() -> Unit)? = null,
    private val imageBuilderConfig: (ImageCapture.Builder.() -> Unit)? = null,
    private val errorCallbacks: ErrorCallbacks? = null,
) {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("CameraXController")

    private val context: Context by lazy {
        previewView.context
    }

    private val activity: Activity by lazy {
        previewView.asActivityOrThrow()
    }

    private val lifecycleOwner: LifecycleOwner by lazy {
        context.lifecycleOwnerOrThrow()
    }

    private val imageCaptureExecutor = Executors.newSingleThreadExecutor()

    private val _cameraStateType = MutableLiveData(CameraState.Type.CLOSED)

    /**
     * Текущий тип состояния камеры, сообщаемый из cameraInfo
     */
    val cameraStateType = _cameraStateType as LiveData<CameraState.Type>

    private val _cameraLoadState = MutableLiveData(LoadState.success(Unit))

    /**
     * Текущее загрузочное состояние камеры:
     * 1. Loading - в процессе подключения/отключения
     * 2. Success - успешное подключение/отключение
     * 3. Error - ошибка, при которой подключение не удалось или камера перестала быть [CameraState.Type.OPEN]
     */
    val cameraLoadState = _cameraLoadState as LiveData<LoadState<Unit>>

    private val orientationIntervalListener =
        object : DiffOrientationEventListener(context, notifyDiffThreshold = 25) {
            override fun onCorrectedRotationChanged(correctedRotation: Int) {
                val rotation = getSurfaceRotation(getCorrectedRotationDegreesForCameraX(correctedRotation))
//                В Preview не влияет на отображение фреймов,
//                т.к. CameraX автоматически корректирует визуальное представление
//                preview?.targetRotation = rotation
                imageCapture?.targetRotation = rotation
                imageAnalysis?.targetRotation = rotation
            }
        }

    val isCameraOpened: Boolean
        get() {
            if (camera == null || cameraProvider == null) return false
            val value = camera?.cameraInfo?.cameraState?.value?.type
            return value == CameraState.Type.OPEN
        }

    val isCameraClosed: Boolean
        get() {
            if (camera == null || cameraProvider == null) return true
            val value = camera?.cameraInfo?.cameraState?.value?.type
            return value == CameraState.Type.CLOSED
        }

    val cameraFacing: CameraFacing?
        get() = camera?.let {
            CameraFacing.resolveByCameraX(it.cameraInfo.lensFacing)
        }

    val cameraInfo: CameraInfo?
        get() = camera?.cameraInfo

    val cameraControl: CameraControl?
        get() = camera?.cameraControl

    /**
     * LiveData, на которые можно подписаться;
     * Появляются в OPEN состоянии
     */
    var observables: CameraObservables? = null
        private set

    private var isPendingStart = false

    private var isPendingClose = false

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private var camera: Camera? = null
        @SuppressLint("MissingPermission")
        set(value) {
            field = value
            if (value != null) {
                field?.cameraInfo?.cameraState?.removeObservers(lifecycleOwner)

                value.cameraInfo.cameraState.observe(lifecycleOwner) {
                    logger.d("Camera state changed: $it")
                    _cameraStateType.value = it.type

                    when (it.type) {
                        CameraState.Type.OPEN -> {
                            orientationIntervalListener.enable()
                            _cameraLoadState.successLoad(Unit)
                        }

                        CameraState.Type.CLOSED -> {
                            orientationIntervalListener.disable()
                            _cameraLoadState.successLoad(Unit)

                            if (isPendingClose) {
                                // close может быть при первом подключении к камере и при onStop lifecycle,
                                // обнулять при этом не надо
                                isPendingClose = false

                                cameraProvider = null
                                camera = null
                                preview = null
                                imageCapture = null
                                imageAnalysis = null
                                observables = null

                                if (isPendingStart) {
                                    isPendingStart = false
                                    startCamera()
                                }
                            }
                        }

                        else -> {
                            // do nothing
                        }
                    }

                    it.error?.let { error ->
                        logger.e("Camera error occurred: $error")
                        if (!isCameraOpened) {
                            _cameraLoadState.errorLoad(error.cause ?: Exception())
                        }
                        errorCallbacks?.onCameraStateError(error)
                    }
                }

                // из актуальной camera взять LiveData
                observables = CameraObservables(
                    if (value.cameraInfo.hasFlashUnit()) {
                        value.cameraInfo.torchState.map { state ->
                            TorchState.resolve(state)
                        }
                    } else {
                        TorchState.NONE.just()
                    },
                    value.cameraInfo.zoomState
                )
            } else {
                field?.cameraInfo?.cameraState?.removeObservers(lifecycleOwner)
            }
        }

    private var cameraProvider: ProcessCameraProvider? = null

    @MainThread
    fun closeCamera() {
        if (!isCameraClosed) {
            logger.d("Unbinding camera provider...")
            isPendingClose = true
            _cameraLoadState.loading()
            cameraProvider?.unbindAll()
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @MainThread
    fun restartCamera() {
        logger.d("restartCamera")
        if (!isCameraClosed) {
            closeCamera()
            // дожидаемся корректного закрытия текущей, прежде чем стартовать следующую
            isPendingStart = true
        } else {
            startCamera()
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @MainThread
    fun toggleCamera() {
        logger.d("toggleCamera")
        if (isCameraClosed) {
            startCamera()
        } else {
            closeCamera()
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @MainThread
    fun startCamera() {
        logger.d("startCamera")

        if (!isCameraClosed) {
            logger.e("Cannot start camera: already running")
            return
        }

        fun onStartError(e: Exception) {
            logException(logger, e)
            _cameraLoadState.errorLoad(e)
            errorCallbacks?.onCameraStartError(e)
        }

        // провайдер, получаемый асинхронно, колбек на главном потоке
        val cameraProviderFuture = try {
            ProcessCameraProvider.getInstance(context)
        } catch (e: IllegalStateException) {
            onStartError(e)
            return
        }

        _cameraLoadState.loading()

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get().also {
                    this.cameraProvider = it
                }
                cameraProvider.unbindAll()

                with(UseCaseGroup.Builder()) {

                    createImagePreview().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                        preview = it
                        addUseCase(it)
                    }
                    imageCapture = createImageCapture().also {
                        addUseCase(it)
                    }
                    imageAnalysis = createImageAnalysis().also {
                        if (it != null) {
                            addUseCase(it)
                        }
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacingProvider().toCameraXValue())
                        .build()

                    previewView.viewPort?.let {
                        setViewPort(it)
                    }

                    // без активного пермишна камера не откроется молча без ошибок
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        this.build()
                    )
                }
            } catch (e: Exception) {
                onStartError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * @param imageConfig меняет ранее инициализированный [ImageCapture] нужными параметрами
     */
    @MainThread
    fun takePicture(
        storageType: StorageType,
        resourceNameFunc: (Long) -> String = { it.toString() },
        imageConfig: (ImageCapture.() -> Unit)? = null,
    ): LiveData<LoadState<Uri>> {
        logger.d("takePicture, storageType: $storageType")

        if (!isCameraOpened) {
            logger.e("Cannot take picture: camera is not opened")
            return LoadState.error<Uri>(IllegalStateException()).just()
        }

        val capture = imageCapture ?: return LoadState.error<Uri>(IllegalStateException()).just()

        val storage = ContentStorage.createUriStorage(storageType, ContentType.IMAGE, context)

        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = cameraFacing == CameraFacing.FRONT
        }

        val resource = try {
            storage.openOutputStream(
                resourceNameFunc(System.currentTimeMillis()).appendExtension("jpg")
            ).get()
        } catch (e: Exception) {
            logger.e("Cannot take picture: openOutputStream failed", e)
            return LoadState.error<Uri>(e).just()
        }

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(resource.second)
            .setMetadata(metadata)
            .build()
        imageConfig?.invoke(capture)

        val result = MutableLiveData<LoadState<Uri>>(LoadState.loading())

        capture.takePicture(
            outputFileOptions,
            imageCaptureExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    logger.d("onImageSaved, outputFileResults: $outputFileResults")
                    // результат приходит на треде от executor;
                    // savedUri нульное, т.к. подставляется стрим
                    result.successLoad(resource.first, false)
                }

                override fun onError(exception: ImageCaptureException) {
                    logger.e("onError, exception: $exception")
                    resource.first.delete(context.contentResolver)
                    result.errorLoad(exception, false)
                }
            }
        )

        return result
    }

    private fun createImagePreview() = Preview.Builder()
        .setResolutionSelector(resolutionSelector())
        .setTargetRotation(previewView.display.rotation)
        .apply { previewBuilderConfig?.invoke(this) }
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

    private fun createImageAnalysis(): ImageAnalysis? {
        val analyzer = imageAnalyzerProvider?.invoke() ?: return null
        return ImageAnalysis.Builder()
            .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { setAnalyzer(imageAnalyzerExecutor, analyzer) }
    }

    private fun createImageCapture() =
        ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector())
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .apply { imageBuilderConfig?.invoke(this) }
            .build()

    private fun resolutionSelector(): ResolutionSelector {
        return ResolutionSelector.Builder().apply {
            setAspectRatioStrategy(
                AspectRatioStrategy(
                    if (activity.getDisplayStandardAspectRatio() == StandardAspectRatio._4_3) {
                        AspectRatio.RATIO_4_3
                    } else {
                        AspectRatio.RATIO_16_9
                    },
                    FALLBACK_RULE_AUTO
                )
            )
            setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
        }.build()
    }

    enum class TorchState(val value: Int) {

        ON(androidx.camera.core.TorchState.ON),
        OFF(androidx.camera.core.TorchState.OFF),
        NONE(-1);

        companion object {

            @JvmStatic
            fun resolve(value: Int) = entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown value: $value")
        }
    }

    class CameraObservables(
        val torchState: LiveData<TorchState>,
        val zoomState: LiveData<ZoomState>,
    )

    interface ErrorCallbacks {

        fun onCameraStartError(e: Exception)

        fun onCameraStateError(e: CameraState.StateError)
    }
}