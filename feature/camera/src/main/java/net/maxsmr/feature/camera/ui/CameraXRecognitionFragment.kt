package net.maxsmr.feature.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.graphic.createBitmapFromUri
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.live.observeLoadStateOnce
import net.maxsmr.commonutils.live.zip
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.views.setShowProgress
import net.maxsmr.feature.camera.CameraFacing
import net.maxsmr.feature.camera.CameraXController
import net.maxsmr.feature.camera.CameraXController.ErrorCallbacks
import net.maxsmr.feature.camera.R
import net.maxsmr.feature.camera.databinding.FragmentCameraXBinding
import net.maxsmr.feature.camera.databinding.LayoutCameraControlsBinding
import net.maxsmr.feature.camera.recognition.cases.BankCardTextMatcherNumberUseCase
import net.maxsmr.feature.camera.recognition.cases.DocTypeTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.EmailTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.ExactTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.GrzTextMatcherUseCase
import net.maxsmr.feature.camera.recognition.cases.RusPhoneTextMatcherUseCase
import net.maxsmr.feature.camera.utils.toggleRequestedOrientationByState
import net.maxsmr.permissionchecker.PermissionsHelper
import java.lang.String.join
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class CameraXRecognitionFragment : BaseNavigationFragment<CameraXRecognitionViewModel>(), ErrorCallbacks {

    override val layoutId: Int = R.layout.fragment_camera_x

    override val viewModel: CameraXRecognitionViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            factory.create(
                it,
                Executors.newSingleThreadExecutor(),
                listOf(
                    BankCardTextMatcherNumberUseCase(),
                    RusPhoneTextMatcherUseCase(),
                    EmailTextMatcherUseCase(),
                    GrzTextMatcherUseCase(),
                    DocTypeTextMatcherUseCase(),
                    ExactTextMatcherUseCase("Camera X")
                )
            )
        }
    }

    override val menuResId: Int = R.menu.menu_camera_x

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private val binding by viewBinding(FragmentCameraXBinding::bind)

    @Inject
    lateinit var factory: CameraXRecognitionViewModel.Factory

    private val controller: CameraXController by lazy {
        CameraXController(
            binding.containerPreview.containerPreview.previewView,
            viewModel.imageAnalyzerExecutor,
            imageBuilderConfig = {
                setFlashMode(FLASH_MODE_AUTO)
            },
            lensFacingProvider = {
                viewModel.cameraFacingField.value ?: CameraFacing.BACK
            },
            imageAnalyzerProvider = {
                ImageAnalysis.Analyzer {
                    viewModel.onFrameReceived(it)
                }
            },
            errorCallbacks = this
        )
    }

    private var toggleFlashLightMenuItem: MenuItem? = null
    private var toggleRecognitionMenuItem: MenuItem? = null

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: CameraXRecognitionViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        with(LayoutCameraControlsBinding.bind(binding.containerControls)) {
            val adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.camera_facing_values,
                android.R.layout.simple_list_item_1
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCameraFacing.adapter = adapter
            spinnerCameraFacing.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.cameraFacingField.value = if (position == 0) {
                        null
                    } else {
                        CameraFacing.entries[position - 1]
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            controller.cameraStateType.observe {
                val isOpened = controller.isCameraOpened

                toggleRequestedOrientationByState(isOpened)

                btToggleCameraState.setText(
                    if (controller.isCameraClosed) {
                        R.string.camera_open
                    } else {
                        R.string.camera_close
                    }
                )
                btCameraTakePicture.isEnabled = isOpened

//                if (isOpened && controller.cameraInfo?.hasFlashUnit() != true) {
//                    viewModel.flashLightState.value = null
//                }

                if (!isOpened) {
                    viewModel.flashLightStateLiveData.value = null
                    viewModel.clearStatsData()
                }

                controller.observables?.let {
                    if (isOpened) {
                        it.torchState.observe { state ->
                            viewModel.flashLightStateLiveData.value = when (state) {
                                CameraXController.TorchState.ON -> true
                                CameraXController.TorchState.OFF -> false
                                else -> null
                            }
                        }
//                        it.zoomState.observe(viewLifecycleOwner) {}
                    }

                    if (controller.isCameraClosed) {
                        it.torchState.removeObservers(viewLifecycleOwner)
//                        it.zoomState.removeObservers(viewLifecycleOwner)
                    }
                }
            }

            viewModel.cameraFacingField.valueLive.observe {
                if (it != null) {
                    spinnerCameraFacing.setSelection(it.ordinal + 1)
                } else {
                    spinnerCameraFacing.setSelection(0)
                }
                if (it == null) {
                    controller.closeCamera()
                } else if (controller.cameraFacing != it && controller.isCameraOpened) {
                    // переоткрытие камеры (если открыта) при несовпадении текущего facing
                    doOnPermissionsResult(
                        BaseActivity.REQUEST_CODE_PERMISSION_CAMERA,
                        listOf(Manifest.permission.CAMERA)
                    ) {
                        controller.restartCamera()
                    }
                }
                btToggleCameraState.isEnabled = it != null
            }

            btToggleCameraState.setOnClickListener {
                doOnPermissionsResult(
                    BaseActivity.REQUEST_CODE_PERMISSION_CAMERA,
                    listOf(Manifest.permission.CAMERA)
                ) {
                    controller.toggleCamera()
                }
            }
            btCameraTakePicture.setOnClickListener {
                controller.takePicture(ContentStorage.StorageType.SHARED).observeLoadStateOnce(viewLifecycleOwner) {
                    btCameraTakePicture.setShowProgress(it.isLoading, defaultDrawableResId = R.drawable.ic_capture)
                    btCameraTakePicture.isEnabled = !it.isLoading && controller.isCameraOpened
                    if (!it.isLoading) {
                        if (it.isSuccessWithData()) {
                            createBitmapFromUri(it.data!!, requireContext().contentResolver)?.let { bitmap ->
                                viewModel.onImageCaptured(bitmap)
                            }
                        } else {
                            it.error?.let { e ->
                                viewModel.showTakePictureError(e)
                            }
                        }
                    }
                }
            }
        }
        with(binding.containerPreview) {
            controller.cameraLoadState.observe {
                if (it.isLoading) {
                    pbPreview.isVisible = true
                    containerPreview.previewView.isVisible = false
                    containerError.root.isVisible = false
                } else {
                    pbPreview.isVisible = false
                    if (it.isSuccess()) {
                        containerPreview.previewView.isVisible = true
                        containerError.root.isVisible = false
                    } else {
                        containerPreview.previewView.isVisible = false
                        containerError.root.isVisible = true
                        containerError.tvEmptyError.setTextOrGone(it.error?.message)
                    }
                }
            }
            viewModel.frameStatsLiveData.observe {
                containerPreview.tvFpsInfo.setTextOrGone(it?.lastFps?.roundToInt()?.let { fps ->
                    getString(R.string.camera_fps_text_format, fps)
                })
            }
            zip(viewModel.recognitionStateLiveData, viewModel.realtimeResultsLiveData) { state, results ->
                Pair(state, results)
            }.observe {
                val results = it.second
                if (results != null && it.first == true) {
                    if (results is CameraXRecognitionViewModel.TextRecognitionResult.Success) {
                        containerPreview.tvRecognitionSuccessResult.text = results.message.get(requireContext())
                        containerPreview.tvRecognitionSuccessResult.isVisible = true
                        containerPreview.tvRecognitionFailureResult.isVisible = false
                    } else {
                        results as CameraXRecognitionViewModel.TextRecognitionResult.Failed
                        val textResult = mutableListOf<String>().apply {
                            results.sourceText?.let { text ->
                                add(text)
                            }
                            results.exception.message?.let { message ->
                                add(message)
                            }
                        }.mapNotNull { it.takeIf { it.isNotEmpty() } }
                        containerPreview.tvRecognitionFailureResult.setTextOrGone(if (textResult.isNotEmpty()) {
                            getString(R.string.camera_recognize_text_failed_format, join("\n\n", textResult))
                        } else {
                            EMPTY_STRING // getString(R.string.camera_recognize_text_failed)
                        })
                        containerPreview.tvRecognitionFailureResult.isVisible = true
                    }
                } else {
                    containerPreview.tvRecognitionFailureResult.isVisible = false
                    containerPreview.tvRecognitionSuccessResult.isVisible = false
                }
            }
            containerError.btRetry.setOnClickListener {
                if (controller.isCameraClosed) {
                    doOnPermissionsResult(
                        BaseActivity.REQUEST_CODE_PERMISSION_CAMERA,
                        listOf(Manifest.permission.CAMERA)
                    ) {
                        controller.startCamera()
                    }
                }
            }
        }

        viewModel.flashLightStateLiveData.observe {
            refreshToggleFlashLightItem(it)
        }
        zip(viewModel.recognitionStateLiveData, controller.cameraStateType) { recognitionState, cameraState ->
            Pair(recognitionState, cameraState)
        }.observe {
            refreshToggleRecognitionMenuItemItem(it.first ?: false, it.second == CameraState.Type.OPEN)
        }
        viewModel.recognitionStateLiveData.observe {
            with(requireActivity().window) {
                if (it) {
                    addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        toggleFlashLightMenuItem = menu.findItem(R.id.actionToggleFlash)
        toggleRecognitionMenuItem = menu.findItem(R.id.actionToggleRecognition)
        refreshToggleFlashLightItem(viewModel.flashLightStateLiveData.value)
        refreshToggleRecognitionMenuItemItem(
            viewModel.recognitionStateLiveData.value ?: false,
            controller.isCameraOpened
        )
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.actionToggleFlash -> {
                viewModel.flashLightStateLiveData.value?.let {
                    controller.cameraControl?.enableTorch(!it)
                }
                true
            }

            R.id.actionToggleRecognition -> {
                val state = viewModel.recognitionStateLiveData.value ?: false
                viewModel.setRecognitionState(!state)
                true
            }

            else -> {
                false
            }
        }
    }

    override fun onCameraStartError(e: Exception) {
        viewModel.showCameraOpenError(e)
    }

    override fun onCameraStateError(e: CameraState.StateError) {
        viewModel.showCameraStateError(e)
    }

    private fun refreshToggleFlashLightItem(state: Boolean?) {
        toggleFlashLightMenuItem?.let { item ->
            if (state != null) {
                item.setIcon(if (state) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
                item.setTitle((if (state) R.string.camera_menu_action_toggle_flashlight_off else R.string.camera_menu_action_toggle_flashlight_on))
                item.isVisible = true
            } else {
                item.isVisible = false
            }
        }
    }

    private fun refreshToggleRecognitionMenuItemItem(state: Boolean, isCameraOpened: Boolean) {
        toggleRecognitionMenuItem?.let { item ->
            if (isCameraOpened) {
                item.setIcon(if (state) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                item.setTitle((if (state) R.string.camera_menu_action_toggle_recognition_stop else R.string.camera_menu_action_toggle_recognition_start))
                item.isVisible = true
            } else {
                item.isVisible = false
            }
        }
    }
}