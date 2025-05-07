package net.maxsmr.feature.camera.recognition.cases

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.graphic.isBitmapValid
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.coroutines.execute.usecase.UseCase
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine.Companion.joinLines
import net.maxsmr.feature.camera.recognition.ITextRecognition
import javax.inject.Inject

class ImageCaptureRecognitionUseCase @Inject constructor(
    private val textRecognition: ITextRecognition,
): UseCase<ImageCaptureRecognitionUseCase.Params, TextMessage>(Dispatchers.Default) {

    override suspend fun execute(parameters: Params): TextMessage {
        return with(parameters) {
            try {
                if (!isBitmapValid(imageBitmap)) {
                    throw IllegalArgumentException("imageBitmap is not valid")
                }
                val result = textRecognition.processCapture(imageBitmap, rotationDegrees)
                if (result.isEmpty()) {
                    throw EmptyResultException()
                }
                TextMessage(result.joinLines())
            } finally {
                imageBitmap.recycle()
            }
        }
    }

    data class Params(
        val imageBitmap: Bitmap,
        val rotationDegrees: Int
    )
}