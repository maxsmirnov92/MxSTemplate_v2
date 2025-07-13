package net.maxsmr.feature.camera.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.view.alert.delegate.FragmentViewAlertDelegate
import net.maxsmr.feature.camera.Camera2Controller
import net.maxsmr.feature.camera.Camera2Controller.CameraState
import net.maxsmr.feature.camera.CameraFacing
import net.maxsmr.feature.camera.R
import net.maxsmr.feature.camera.databinding.FragmentCamera2Binding
import net.maxsmr.feature.camera.databinding.LayoutCameraControlsBinding
import net.maxsmr.feature.camera.utils.toggleRequestedOrientationByState
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class Camera2Fragment : BaseNavigationFragment<Camera2ViewModel>() {

    override val layoutId: Int = R.layout.fragment_camera_2

    override val viewModel: Camera2ViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private val binding by viewBinding(FragmentCamera2Binding::bind)

    private val controller: Camera2Controller by lazy {
        Camera2Controller(binding.textureView)
    }

    override fun createAlertDelegate() = FragmentViewAlertDelegate(this, viewModel)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: Camera2ViewModel) {
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
            viewModel.cameraFacingField.valueFlow.observeSafe {
                if (it != null) {
                    spinnerCameraFacing.setSelection(it.ordinal + 1)
                } else {
                    spinnerCameraFacing.setSelection(0)
                }
                if (it == null) {
                    controller.closeCamera()
                } else if (controller.cameraFacing.value != it) {
                    doOnPermissionsResult(
                        BaseActivity.REQUEST_CODE_PERMISSION_CAMERA,
                        listOf(Manifest.permission.CAMERA)
                    ) {
                        controller.params = Camera2Controller.Params(facing = it)
                    }
                }
                btToggleCameraState.isEnabled = it != null
            }
            combine(controller.cameraId, controller.state) { id, state ->
                Pair(id, state)
            }.observeSafe { (id, state) ->
                tvCameraState.text = if (id.isNullOrEmpty()) {
                    state.name
                } else {
                    "${state.name} (${getString(R.string.camera_id_format, id)})"
                }

                toggleRequestedOrientationByState(controller.isCameraOpened)

                btToggleCameraState.setText(
                    if (state == CameraState.NOT_INITIALIZED) {
                        R.string.camera_open
                    } else {
                        R.string.camera_close
                    }
                )
                btCameraTakePicture.isEnabled = state == CameraState.PREVIEWING
//                if (state == CameraState.NOT_INITIALIZED) {
//                    binding.textureView.alpha = 0f
//                }
            }
//            controller.cameraFacing.observeSafe {
//                viewModel.cameraFacingField.value = it
//            }
            btToggleCameraState.setOnClickListener {
                if (controller.isCameraOpened) {
                    controller.closeCamera()
                } else {
                    doOnPermissionsResult(
                        BaseActivity.REQUEST_CODE_PERMISSION_CAMERA,
                        listOf(Manifest.permission.CAMERA)
                    ) {
                        controller.openCamera()
                    }
                }
            }
            btCameraTakePicture.setOnClickListener {
                controller.takePicture(ContentStorage.StorageType.SHARED)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        doOnPermissionsResult(BaseActivity.REQUEST_CODE_PERMISSION_CAMERA, listOf(Manifest.permission.CAMERA)) {
            controller.onStart(viewModel.cameraFacingField.value != null)
        }
    }

    override fun onPause() {
        super.onPause()
        controller.onStop()
    }
}