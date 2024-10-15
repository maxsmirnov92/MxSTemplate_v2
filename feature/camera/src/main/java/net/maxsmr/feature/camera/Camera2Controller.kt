package net.maxsmr.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.Surface.OutOfResourcesException
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import androidx.core.os.ExecutorCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maxsmr.commonutils.gui.DiffOrientationEventListener
import net.maxsmr.commonutils.gui.DiffOrientationEventListener.Companion.ROTATION_NOT_SPECIFIED
import net.maxsmr.commonutils.isAtLeastPie
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.stream.ByteBufferBackedInputStream
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.ContentStorage.StorageType
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO колбеки для клиентского кода
class Camera2Controller(private val textureView: TextureView) {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("CameraController")

    private val context: Context by lazy { textureView.context }

    private val manager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    val isCameraOpened get() = cameraDevice != null && _state.value != CameraState.NOT_INITIALIZED

    var params: Params = Params()
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            if (value != field) {
                // целевые парамсы изменились, переоткрытие камеры
                field = value
                if (isCameraOpened) {
                    reopenCamera()
                }
            }
        }

    private var _state = MutableStateFlow(CameraState.NOT_INITIALIZED)

    var state = _state.asStateFlow()

    private var _cameraId = MutableStateFlow<String?>(null)

    var cameraId = _cameraId.asStateFlow()

    private var _cameraFacing = MutableStateFlow<CameraFacing?>(null)

    var cameraFacing = _cameraFacing.asStateFlow()

    private var cameraDevice: CameraDevice? = null

    private var imageReader: ImageReader? = null

    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundExecutor: Executor? = null

    private val textureListener: SurfaceTextureListener = object : SurfaceTextureListener {

        @SuppressLint("MissingPermission")
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //open your camera here
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            closeCamera()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }

    private val cameraStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            logger.d("onOpened")
            cameraDevice = camera
            _state.value = CameraState.OPENED
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            logger.w("onDisconnected, camera: $camera")
            closeCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            logger.e("onError, camera: $camera, error: $error")
            closeCamera()
        }
    }

    private val orientationIntervalListener =
        DiffOrientationEventListener(textureView.context, notifyDiffThreshold = 25)

    @RequiresPermission(Manifest.permission.CAMERA)
    @MainThread
    fun onStart(shouldOpenCamera: Boolean) {
        startBackgroundThread()
        if (shouldOpenCamera && !isCameraOpened && textureView.isAvailable) {
            openCamera()
        }
    }

    @MainThread
    fun onStop() {
        stopBackgroundThread()
        // во избежание "Handler sending message to a Handler on a dead thread"
        closeCamera()
    }

    @Synchronized
    @RequiresPermission(Manifest.permission.CAMERA)
    fun openCamera() {
        logger.d("openCamera")

        if (isCameraOpened) {
            logger.w("Camera is already opened")
            return
        }

        if (textureView.surfaceTextureListener == null) {
            textureView.surfaceTextureListener = textureListener
        }

        if (!textureView.isAvailable) {
            logger.w("TextureView is not available")
            return
        }

        try {
            var cameraId: String? = null

            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: continue
                if (CameraFacing.resolve(facing) == params.facing) {
                    cameraId = id
                    break
                }
            }

            cameraId = cameraId?.takeIf { it.isNotEmpty() } ?: manager.cameraIdList.getOrNull(0)

            if (cameraId == null) {
                logger.w("No available cameras")
                return
            }

            val characteristics = manager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                ?: throw IllegalStateException("Cannot determine camera facing")

            manager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
            _cameraId.value = cameraId
            _cameraFacing.value = CameraFacing.resolve(facing)
                ?: throw IllegalArgumentException("Unknown camera facing")

            logger.d("openCamera success")
        } catch (e: CameraAccessException) {
            logException(logger, e, "openCamera failed")
        }
    }

    @Synchronized
    fun closeCamera() {
        imageReader?.let {
            logger.d("Closing imageReader...")
            it.close()
            imageReader = null
        }
        cameraDevice?.let {
            logger.d("Closing camera...")
            it.close()
            cameraDevice = null
        }
        _cameraId.value = null
        _cameraFacing.value = null
        _state.value = CameraState.NOT_INITIALIZED
        textureView.surfaceTextureListener = null
        orientationIntervalListener.disable()
    }

    @Synchronized
    @RequiresPermission(Manifest.permission.CAMERA)
    fun reopenCamera() {
        closeCamera()
        openCamera()
    }

    @Synchronized
    fun takePicture(
        storageType: StorageType,
        params: Params.MetadataParams = Params.MetadataParams(),
        resourceNameFunc: (Long) -> String = { it.toString() },
    ) {
        logger.d("takePicture")

        val device = cameraDevice ?: run {
            logger.w("Camera is not opened")
            return
        }

        if (_state.value != CameraState.PREVIEWING) {
            logger.w("Camera is not previewing")
            return
        }

        val cameraId = _cameraId.value ?: run {
            logger.w("cameraId is missing")
            return
        }

        val characteristics = manager.getCameraCharacteristics(cameraId)
        val topSize = characteristics.getPictureSizes().getOrNull(0) ?: run {
            logger.w("Cannot determine preview sizes")
            return
        }
        val width = topSize.width
        val height = topSize.height

        val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1).also {
            imageReader = it
        }
        val outputSurfaces = mutableListOf<Surface>()
        outputSurfaces.add(reader.surface)

        try {
            outputSurfaces.add(Surface(textureView.surfaceTexture))
        } catch (e: OutOfResourcesException) {
            logException(logger, e)
            return
        }

        try {
            val readerListener = OnImageAvailableListener {
                val storage = ContentStorage.createUriStorage(storageType, ContentType.IMAGE, context)
                try {
                    it.acquireLatestImage().use { image ->
                        val buffer = image.planes[0].buffer
                        storage.write(
                            ByteBufferBackedInputStream(buffer),
                            resourceNameFunc(System.currentTimeMillis()).appendExtension("jpg")
                        ).get()
                    }
                } catch (e: Exception) {
                    logException(logger, e)
                }
            }
            reader.setOnImageAvailableListener(readerListener, backgroundHandler)

            val captureStateCallback = object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice !== device || _state.value != CameraState.TAKING_PICTURE) {
                        return
                    }

                    val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    builder.addTarget(reader.surface)
                    builder.set(CaptureRequest.CONTROL_MODE, params.controlMode.value)
                    builder.set(
                        CaptureRequest.JPEG_ORIENTATION,
                        getCorrectedRotationDegreesForCamera(
                            _cameraFacing.value == CameraFacing.FRONT,
                            orientationIntervalListener.lastCorrectedRotation
                        )
                    )

                    val captureListener: CaptureCallback = object : CaptureCallback() {

                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {
                            super.onCaptureCompleted(session, request, result)
                            logger.i("onCaptureCompleted")
                            createCameraPreview()
                        }
                    }

                    try {
                        session.capture(builder.build(), captureListener, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        logException(logger, e, "capture")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    logger.e("onConfigureFailed")
                    _state.value = CameraState.PREVIEWING
                }
            }

            val executor = backgroundExecutor
            if (isAtLeastPie() && executor != null) {
                device.createCaptureSession(
                    SessionConfiguration(
                        params.sessionMode.toNativeSessionMode(),
                        outputSurfaces.map { OutputConfiguration(it) },
                        executor,
                        captureStateCallback
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                device.createCaptureSession(outputSurfaces, captureStateCallback, backgroundHandler)
            }
            _state.value = CameraState.TAKING_PICTURE

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun createCameraPreview() {
        logger.d("createCameraPreview")

        val device = cameraDevice ?: throw IllegalStateException("Cannot create preview: camera is not opened")

        if (_state.value == CameraState.PREVIEWING) {
            logger.w("Camera is already previewing")
            return
        }

        val cameraId = _cameraId.value ?: run {
            logger.w("cameraId is missing")
            return
        }

        val texture: SurfaceTexture = checkNotNull(textureView.surfaceTexture)
        val characteristics = manager.getCameraCharacteristics(cameraId)
        textureView.setTextureTransform(characteristics)
//        characteristics.getPreviewSize()?.let {
        // так будет работать только в двух случаях из 4-х
        // в landscape-ориентации нужен setTransform(matrix)
//            texture.setDefaultBufferSize(it.width, it.height)
//            val rotationSize = textureView.getRotationSizeFromView(characteristics)
//            textureView.post {
//                textureView.scaleByContent(rotationSize, true)
//            }
//        }
        val surface = try {
            Surface(texture)
        } catch (e: OutOfResourcesException) {
            logException(logger, e)
            return
        }

        fun updatePreview(session: CameraCaptureSession) {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)
            builder.set(CaptureRequest.CONTROL_MODE, params.metadataParams.controlMode.value)
            try {
                session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                _state.value = CameraState.PREVIEWING
                orientationIntervalListener.enable()
            } catch (e: CameraAccessException) {
                logException(logger, e, "setRepeatingRequest")
            }
        }

        val captureStateCallback = object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                logger.d("onConfigured")
                if (cameraDevice !== device) {
                    return
                }
                // When the session is ready, we start displaying the preview.
                updatePreview(cameraCaptureSession)
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                logger.e("onConfigureFailed, cameraCaptureSession: $cameraCaptureSession")
            }
        }

        try {
            val executor = backgroundExecutor
            if (isAtLeastPie() && executor != null) {
                device.createCaptureSession(
                    SessionConfiguration(
                        params.metadataParams.sessionMode.toNativeSessionMode(),
                        listOf(OutputConfiguration(surface)),
                        executor,
                        captureStateCallback
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                device.createCaptureSession(
                    listOf(surface),
                    captureStateCallback,
                    backgroundHandler
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun isBackgroundThreadRunning() = backgroundThread?.isAlive == true

    private fun startBackgroundThread() {
        if (isBackgroundThreadRunning()) return
        with(HandlerThread("Camera Background")) {
            logger.d("Starting background thread...")
            start()
            backgroundHandler = Handler(looper).also {
                if (isAtLeastPie()) {
                    backgroundExecutor = ExecutorCompat.create(it)
                }
            }
            backgroundThread = this
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.let {
            logger.d("Stopping background thread...")
            it.quitSafely()
            try {
                it.join()
                backgroundThread = null
                backgroundHandler = null
                backgroundExecutor = null
            } catch (e: InterruptedException) {
                logException(logger, e, "join")
            }
        }
    }

    data class Params(
        val facing: CameraFacing = CameraFacing.BACK,
        val metadataParams: MetadataParams = MetadataParams(),
    ) {

        data class MetadataParams(
            val sessionMode: SessionMode = SessionMode.REGULAR,
            val controlMode: ControlMode = ControlMode.AUTO,
        )
    }

    enum class CameraFacing(val value: Int) {

        BACK(CameraCharacteristics.LENS_FACING_BACK),
        FRONT(CameraCharacteristics.LENS_FACING_FRONT);

        companion object {

            @JvmStatic
            internal fun resolve(id: Int) = entries.find { it.value == id }
        }
    }

    enum class SessionMode {

        REGULAR,
        HIGH_SPEED;

        @TargetApi(Build.VERSION_CODES.P)
        fun toNativeSessionMode() = when (this) {
            REGULAR -> SessionConfiguration.SESSION_REGULAR
            HIGH_SPEED -> SessionConfiguration.SESSION_HIGH_SPEED
        }
    }

    enum class ControlMode(val value: Int) {

        OFF(CameraMetadata.CONTROL_MODE_OFF),
        AUTO(CameraMetadata.CONTROL_MODE_AUTO),
        SCENE_MODE(CameraMetadata.CONTROL_MODE_USE_SCENE_MODE),
        OFF_KEEP_STATE(CameraMetadata.CONTROL_MODE_OFF_KEEP_STATE),

        @TargetApi(Build.VERSION_CODES.R)
        USE_EXTENDED_SCENE_MODE(CameraMetadata.CONTROL_MODE_USE_EXTENDED_SCENE_MODE)
    }

    enum class CameraState {

        NOT_INITIALIZED,
        OPENED,
        PREVIEWING,
        TAKING_PICTURE,
        RECORDING_VIDEO
    }

}