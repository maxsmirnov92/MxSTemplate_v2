package net.maxsmr.feature.camera.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.field.createField
import net.maxsmr.feature.camera.CameraFacing

class Camera2ViewModel(state: SavedStateHandle): BaseViewModel(state) {

    val cameraFacingField: Field<CameraFacing?> = createField(
        initialValue = null,
        key = KEY_FIELD_CAMERA_FACING
    ) {
        emptyIf { it == null }
    }

    companion object {

        private const val KEY_FIELD_CAMERA_FACING = "camera_facing"
    }
}