package net.maxsmr.feature.camera.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.camera.CameraFacing

class Camera2ViewModel(state: SavedStateHandle): BaseHandleableViewModel(state) {

    val cameraFacingField: Field<CameraFacing?> = Field.Builder<CameraFacing?>(null)
        .emptyIf { false }
        .persist(state, KEY_FIELD_CAMERA_FACING)
        .build()

    companion object {

        private const val KEY_FIELD_CAMERA_FACING = "camera_facing"
    }
}