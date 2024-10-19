package net.maxsmr.feature.camera.recognition

import android.graphics.Bitmap
import android.media.Image
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine
import kotlin.jvm.Throws

interface ITextRecognition {

    val strategy: RecognitionStrategy

    @Throws(Exception::class)
    suspend fun processFrame(frame: Image, rotationDegrees: Int): List<RecognizedLine>

    @Throws(Exception::class)
    suspend fun processCapture(bitmap: Bitmap, rotationDegrees: Int): List<RecognizedLine>

    fun dispose()

    sealed interface RecognitionStrategy {

        data object Local : RecognitionStrategy

        class Remote(val apiKey: String): RecognitionStrategy
    }
}