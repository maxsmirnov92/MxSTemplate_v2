package net.maxsmr.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
import net.maxsmr.commonutils.asActivityOrThrow
import net.maxsmr.commonutils.lifecycleOwnerOrThrow
import net.maxsmr.commonutils.live.errorLoad
import net.maxsmr.commonutils.live.just
import net.maxsmr.commonutils.live.loading
import net.maxsmr.commonutils.live.successLoad
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.ContentStorage.StorageType
import java.util.concurrent.Executors

class CameraXController(
    private val previewView: PreviewView,
    private val lensFacingProvider: () -> CameraFacing = { CameraFacing.BACK },
    private val imageAnalyzerProvider: (() -> ImageAnalysis.Analyzer)? = null,
    private val previewBuilderFunc: (Preview.Builder.() -> Unit)? = null,
    private val imageBuilderFunc: (ImageCapture.Builder.() -> Unit)? = null,
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

    /**
     * Executor для Analyzer и взятия фото
     */
    private val executor = Executors.newSingleThreadExecutor()

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
     * 3. Error - ошибка, при которой подключение не удалось или камера перестала быть OPENED
     */
    val cameraLoadState = _cameraLoadState as LiveData<LoadState<Unit>>

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
            return value == CameraState.Type.CLOSING || value == CameraState.Type.CLOSED
        }

    val cameraFacing: CameraFacing?
        get() = camera?.let {
            CameraFacing.resolve(it.cameraInfo.lensFacing)
        }

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

                    if (it.type == CameraState.Type.OPEN) {
                        _cameraLoadState.successLoad(Unit)
                    }

                    if (it.type == CameraState.Type.CLOSED && isPendingClose) {
                        // close может быть при первом подключении к камере и при onStop lifecycle,
                        // обнулять при этом не надо
                        isPendingClose = false
                        cameraProvider = null
                        camera = null
                        preview = null
                        imageCapture = null
                        imageAnalysis = null
                        _cameraLoadState.successLoad(Unit)
                        if (isPendingStart) {
                            isPendingStart = false
                            startCamera()
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
        closeCamera()
        // дожидаемся корректного закрытия текущей, прежде чем стартовать следующую
        isPendingStart = true
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

                createImagePreview().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                    preview = it
                }
                imageCapture = createImageCapture()
                imageAnalysis = createImageAnalysis()

                cameraProvider.unbindAll()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacingProvider().value)
                    .build()

                // без активного пермишна камера не откроется молча без ошибок
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                onStartError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @MainThread
    fun takePicture(
        storageType: StorageType,
        resourceNameFunc: (Long) -> String = { it.toString() },
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

        val result = MutableLiveData<LoadState<Uri>>(LoadState.loading())

        capture.takePicture(
            outputFileOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    logger.d("onImageSaved, outputFileResults: $outputFileResults")
                    // результат приходит на треде от executor;
                    // savedUri нульное, т.к. подставляется стрим
                    result.successLoad(resource.first, false)
                }

                override fun onError(exception: ImageCaptureException) {
                    logger.e("onError, exception: $exception")
                    result.errorLoad(exception, false)
                }
            }
        )

        return result
    }

    private fun createImagePreview() = Preview.Builder()
        .setResolutionSelector(resolutionSelector())
        .setTargetRotation(previewView.display.rotation)
        .apply { previewBuilderFunc?.invoke(this) }
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

    private fun createImageAnalysis() =
        ImageAnalysis.Builder()
            .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { imageAnalyzerProvider?.invoke()?.let { setAnalyzer(executor, it) } }

    private fun createImageCapture() =
        ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector())
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .apply { imageBuilderFunc?.invoke(this) }
            .build()

    private fun resolutionSelector(): ResolutionSelector {
        return ResolutionSelector.Builder().apply {
            setAspectRatioStrategy(
                AspectRatioStrategy(
                    activity.getDisplayAspectRatio(),
                    FALLBACK_RULE_AUTO
                )
            )
            setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
        }.build()
    }

    enum class CameraFacing(val value: Int) {

        BACK(CameraSelector.LENS_FACING_BACK),
        FRONT(CameraSelector.LENS_FACING_FRONT);

        companion object {

            @JvmStatic
            internal fun resolve(id: Int) = entries.find { it.value == id }
        }
    }

    interface ErrorCallbacks {

        fun onCameraStartError(e: Exception)

        fun onCameraStateError(e: CameraState.StateError)
    }
}