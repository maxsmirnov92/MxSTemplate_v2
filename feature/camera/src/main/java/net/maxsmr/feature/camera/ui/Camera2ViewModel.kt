package net.maxsmr.feature.camera.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.field.createField
import net.maxsmr.feature.camera.CameraFacing
import javax.inject.Inject

@HiltViewModel
class Camera2ViewModel @Inject constructor(
    state: SavedStateHandle,
    @ApplicationContext context: Context
): BaseViewModel(state, context) {

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