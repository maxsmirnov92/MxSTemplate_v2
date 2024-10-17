package net.maxsmr.feature.camera.utils

import android.app.Activity
import androidx.camera.core.AspectRatio
import net.maxsmr.commonutils.getDisplaySize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val RATIO_4_3_VALUE = 4.0 / 3.0
private const val RATIO_16_9_VALUE = 16.0 / 9.0

fun Activity.getDisplayAspectRatio(): Int {
    val size = getDisplaySize()
    val previewRatio = max(size.width, size.height).toDouble() / min(size.width, size.height)
    return  if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
        AspectRatio.RATIO_4_3
    } else {
        AspectRatio.RATIO_16_9
    }
}

/**
 * В Camera X targetRotation соответствует углу поворота устройства
 * (display.rotation, который берётся автоматически,
 * или подставляемый вручную из OrienationEventListener при фиксированной ориентации)
 * только с разницей в ландшафтной ориентации: 90 и 270 нужно поменять местами;
 * под капотом уже учитываются повороты и зеркалирование для обеих камер
 */
fun getCorrectedRotationDegreesForCameraX(displayRotationDegrees: Int): Int {
//    0 -> 0
//    90 -> 270
//    180 -> 180
//    270 -> 90
    if (displayRotationDegrees !in arrayOf(0, 90, 180, 270)) {
        return 0
    }
    return if (displayRotationDegrees % 180 == 0) {
        displayRotationDegrees
    } else {
        (displayRotationDegrees + 180) % 360
    }
}