package net.maxsmr.feature.camera.ui

import androidx.camera.core.CameraState
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.camera.CameraFacing
import net.maxsmr.feature.camera.R

class CameraXViewModel(state: SavedStateHandle): BaseHandleableViewModel(state) {

    val cameraFacingField: Field<CameraFacing?> = Field.Builder<CameraFacing?>(CameraFacing.BACK)
        .emptyIf { false }
        .persist(state, KEY_FIELD_CAMERA_FACING)
        .build()

    fun showCameraOpenError(e: Throwable) {
        showSnackbar(TextMessage(R.string.camera_error_open_format, e.message.takeIf { !it.isNullOrEmpty() } ?: e.toString()))
    }

    fun showCameraStateError(e: CameraState.StateError) {
        showSnackbar(TextMessage(R.string.camera_error_state_format, "${e.code} ${e.type}"))
    }

    fun showTakePictureError(e: Throwable) {
        showSnackbar(TextMessage(R.string.camera_error_take_picture_format, e.message.takeIf { !it.isNullOrEmpty() } ?: e.toString()))
    }

    companion object {

        private const val KEY_FIELD_CAMERA_FACING = "camera_facing"
    }

}