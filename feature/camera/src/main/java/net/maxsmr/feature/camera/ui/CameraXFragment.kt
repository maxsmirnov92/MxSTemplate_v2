package net.maxsmr.feature.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.live.observeLoadStateOnce
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
import net.maxsmr.feature.camera.utils.toggleRequestedOrientationByState
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class CameraXFragment : BaseNavigationFragment<CameraXViewModel>(), ErrorCallbacks {

    override val layoutId: Int = R.layout.fragment_camera_x

    override val viewModel: CameraXViewModel by viewModels()

    override val menuResId: Int = R.menu.menu_camera_x

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private val binding by viewBinding(FragmentCameraXBinding::bind)

    private val controller: CameraXController by lazy {
        CameraXController(
            binding.containerPreview.previewView,
            imageBuilderConfig = {
                setFlashMode(FLASH_MODE_AUTO)
            },
            lensFacingProvider = {
                viewModel.cameraFacingField.value ?: CameraFacing.BACK
            }, errorCallbacks = this
        )
    }

    private var flashLightMenuItem: MenuItem? = null

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: CameraXViewModel) {
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

                controller.observables?.let {
                    if (isOpened) {
                        it.torchState.observe { state ->
                            viewModel.flashLightState.value = when(state) {
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

            viewModel.flashLightState.observe {
                refreshFlashLightItem(it)
            }

            btToggleCameraState.setOnClickListener {
                if (controller.isCameraOpened) {
                    controller.closeCamera()
                } else {
                    doOnPermissionsResult(
                        BaseActivity.REQUEST_CODE_PERMISSION_CAMERA,
                        listOf(Manifest.permission.CAMERA)
                    ) {
                        controller.startCamera()
                    }
                }
            }
            btCameraTakePicture.setOnClickListener {
                controller.takePicture(ContentStorage.StorageType.SHARED).observeLoadStateOnce(viewLifecycleOwner) {
                    btCameraTakePicture.setShowProgress(it.isLoading, defaultDrawableResId = R.drawable.ic_capture)
                    btCameraTakePicture.isEnabled = !it.isLoading && controller.isCameraOpened
                    if (it.isError()) {
                        it.error?.let { e ->
                            viewModel.showTakePictureError(e)
                        }
                    }
                }
            }
        }
        with(binding.containerPreview) {
            controller.cameraLoadState.observe {
                if (it.isLoading) {
                    pbPreview.isVisible = true
                    previewView.isVisible = false
                    containerError.root.isVisible = false
                } else {
                    pbPreview.isVisible = false
                    if (it.isSuccess()) {
                        previewView.isVisible = true
                        containerError.root.isVisible = false
                    } else {
                        previewView.isVisible = false
                        containerError.root.isVisible = true
                        containerError.tvEmptyError.setTextOrGone(it.error?.message)
                    }
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
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        flashLightMenuItem = menu.findItem(R.id.actionFlash)
        refreshFlashLightItem(viewModel.flashLightState.value)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.actionFlash -> {
                viewModel.flashLightState.value?.let {
                    controller.cameraControl?.enableTorch(!it)
                }
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

    private fun refreshFlashLightItem(state: Boolean?) {
        flashLightMenuItem?.let { item ->
            if (state != null) {
                item.setIcon(if (state) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
                item.isVisible = true
            } else {
                item.isVisible = false
            }
        }
    }
}