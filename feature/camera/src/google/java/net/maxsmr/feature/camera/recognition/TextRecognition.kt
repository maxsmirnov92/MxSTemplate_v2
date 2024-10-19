package net.maxsmr.feature.camera.recognition

import android.graphics.Bitmap
import android.media.Image

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine
import net.maxsmr.feature.camera.recognition.ITextRecognition.RecognitionStrategy.Local
import net.maxsmr.mobile_services.asCoroutine

class TextRecognition : ITextRecognition {

    override val strategy: ITextRecognition.RecognitionStrategy = Local

    private val analyzer = TextRecognition.getClient(
        TextRecognizerOptions.Builder().build()
    )

    override suspend fun processFrame(frame: Image, rotationDegrees: Int): List<RecognizedLine> {
        return InputImage.fromMediaImage(frame, rotationDegrees).process()
    }

    override suspend fun processCapture(bitmap: Bitmap, rotationDegrees: Int): List<RecognizedLine> {
        return InputImage.fromBitmap(bitmap, rotationDegrees).process()
    }

    override fun dispose() {
        analyzer.close()
    }

    private suspend fun InputImage.process(): List<RecognizedLine> {
        return analyzer.process(this).asCoroutine {
            it.textBlocks.flatMap { block -> block.lines }
                .map { line ->
                    line.toRecognizedLine()
                }
        }
    }

    private fun Text.Line.toRecognizedLine(): RecognizedLine = RecognizedLine(text)
}