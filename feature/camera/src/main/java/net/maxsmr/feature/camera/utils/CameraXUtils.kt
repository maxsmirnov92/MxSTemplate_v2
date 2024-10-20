package net.maxsmr.feature.camera.utils


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