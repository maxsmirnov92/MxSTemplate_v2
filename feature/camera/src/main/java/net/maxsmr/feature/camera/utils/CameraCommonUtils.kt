package net.maxsmr.feature.camera.utils

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Size
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import kotlin.math.abs

fun Fragment.toggleRequestedOrientationByState(isCameraOpened: Boolean) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        // Фиксация ориентации на текущую ориентацию экрана в зав-ти от состояния камеры
        // С API 33 через манифест стало deprecated
        requireActivity().requestedOrientation = if (isCameraOpened) {
            val currentOrientation = resources.configuration.orientation
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

/**
 * Поменять местами width и height при несовпадении
 * ориентации сенсора и дисплея для дальнейшего рескейла view под контент
 */
fun getRotationSize(
    sensorOrientationDegrees: Int,
    displayRotationDegrees: Int,
    previewSize: Size
): Size {
    if (sensorOrientationDegrees > 360
            || displayRotationDegrees > 360
    ) {
        return Size(0, 0)
    }
    return if ((sensorOrientationDegrees - displayRotationDegrees.toFloat()) % 180 == 0f) {
        Size(previewSize.width, previewSize.height)
    } else {
        // Swap the width and height if the sensor orientation and display rotation don't match
        Size(previewSize.height, previewSize.width)
    }
}

/**
 * Скорректировать угол поворота дисплея в соот-ии с представлением камеры
 * (например, для задней камеры 0 начинается, когда экран повёрнут влево в landscape ориентации)
 * @param isFront является ли камера фронтальной
 * @param displayRotationDegrees актуальное значение с OrientationEventListener (при фиксированной ориентации),
 * приведённое к 0, 90, 180, 270,
 * или взятое из display.rotation (при пересоздаваемом экране в зав-ти от ориентации)
 * @param sensorOrientationDegrees фиксированное значение из CameraCharacteristics:
 * например, 270 для задней и 90 для фронтальной камеры
 */
fun getCorrectedRotationDegreesForCamera(
    isFront: Boolean,
    displayRotationDegrees: Int,
    sensorOrientationDegrees: Int,
): Int {
    if (displayRotationDegrees !in arrayOf(0, 90, 180, 270)) {
        return 0
    }
    val result = if (isFront) {
        sensorOrientationDegrees + 180 - displayRotationDegrees
    } else {
        sensorOrientationDegrees - 180 + displayRotationDegrees
    }
    return abs(result) % 360
}