package net.maxsmr.feature.camera;

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.core.CameraSelector

enum class CameraFacing() {

    BACK,
    FRONT;

    fun toCamera2Value() = when (this) {
        BACK -> CameraCharacteristics.LENS_FACING_BACK
        FRONT -> CameraCharacteristics.LENS_FACING_FRONT
    }

    fun toCameraXValue() = when (this) {
        BACK -> CameraSelector.LENS_FACING_BACK
        FRONT -> CameraSelector.LENS_FACING_FRONT
    }

    companion object {

        @JvmStatic
        fun resolveByCamera2(id: Int) = when (id) {
            CameraCharacteristics.LENS_FACING_BACK -> BACK
            CameraCharacteristics.LENS_FACING_FRONT -> FRONT
            else -> throw IllegalArgumentException("Unknown facing id: $id")
        }

        @JvmStatic
        fun resolveByCameraX(id: Int) = when (id) {
            CameraSelector.LENS_FACING_BACK -> BACK
            CameraSelector.LENS_FACING_FRONT -> FRONT
            else -> throw IllegalArgumentException("Unknown facing id: $id")
        }
    }
}