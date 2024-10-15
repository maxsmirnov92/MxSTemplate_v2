package net.maxsmr.feature.camera

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.TextureView
import net.maxsmr.commonutils.gui.getRotationDegrees

fun TextureView.setTextureTransform(characteristics: CameraCharacteristics) {
    val texture = surfaceTexture ?: return
    val previewSize: Size = characteristics.getPreviewSize() ?: return

    val width: Int = previewSize.width
    val height: Int = previewSize.height

    // Indicate the size of the buffer the texture should expect
    texture.setDefaultBufferSize(width, height)
    // Save the texture dimensions in a rectangle
    val viewRect = RectF(0f, 0f, getWidth().toFloat(), getHeight().toFloat())
    // Determine the rotation of the display

    val sensorOrientation: Int = characteristics.getCameraSensorOrientationDegrees()
    val rotationDegrees = getRotationDegrees(display.rotation).toFloat()

    val s = getRotationSize(sensorOrientation, rotationDegrees.toInt(), previewSize)
    val w: Float = s.width.toFloat()
    val h: Float = s.height.toFloat()

    val viewAspectRatio = viewRect.width() / viewRect.height()
    val imageAspectRatio = w / h
    // This will make the camera frame fill the texture view, if you'd like to fit it into the view swap the "<" sign for ">"
    val scale = if (viewAspectRatio < imageAspectRatio) {
        // If the view is "thinner" than the image constrain the height and calculate the scale for the texture width
        PointF((viewRect.height() / viewRect.width()) * (height.toFloat() / width.toFloat()), 1f)
    } else {
        PointF(1f, (viewRect.width() / viewRect.height()) * (width.toFloat() / height.toFloat()))
    }
    if (rotationDegrees % 180 != 0f) {
        // If we need to rotate the texture 90º we need to adjust the scale
        val multiplier = if (viewAspectRatio < imageAspectRatio) w / h else h / w
        scale.x *= multiplier
        scale.y *= multiplier
    }

    val matrix = Matrix()
    // Set the scale
    matrix.setScale(scale.x, scale.y, viewRect.centerX(), viewRect.centerY())
    if (rotationDegrees != 0f) {
        // Set rotation of the device isn't upright
        matrix.postRotate(0 - rotationDegrees, viewRect.centerX(), viewRect.centerY())
    }
    // Transform the texture
    setTransform(matrix)
}

fun TextureView.getRotationSizeFromView(characteristics: CameraCharacteristics): Size? {
    val previewSize: Size = characteristics.getPreviewSize() ?: return null
    return getRotationSize(
        characteristics.getCameraSensorOrientationDegrees(),
        getRotationDegrees(display.rotation),
        previewSize
    )
}

fun CameraCharacteristics.getPreviewSizes(): List<Size> {
    return get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        ?.getOutputSizes(SurfaceTexture::class.java)
        ?.toList().orEmpty()
}

fun CameraCharacteristics.getPictureSizes(): List<Size> {
    return get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        ?.getOutputSizes(ImageFormat.JPEG)
        ?.toList().orEmpty()
}

fun CameraCharacteristics.getPreviewSize(): Size? {
    // TODO: decide on which size fits your view size the best
    return getPreviewSizes().getOrNull(0)
}

/**
 * @return фиксированное значение, которое описывает, как повернут сенсор камеры относительно устройства;
 * Не меняется при смене ориентации дисплея
 */
fun CameraCharacteristics.getCameraSensorOrientationDegrees(): Int {
    val sensorOrientation = get(CameraCharacteristics.SENSOR_ORIENTATION)
    return (360 - (sensorOrientation ?: 0)) % 360
}