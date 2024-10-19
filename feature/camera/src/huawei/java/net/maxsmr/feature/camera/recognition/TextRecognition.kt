package net.maxsmr.feature.camera.recognition

import android.graphics.Bitmap
import android.media.Image
import android.view.Surface
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLApplication
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.text.MLLocalTextSetting
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting
import com.huawei.hms.mlsdk.text.MLText
import com.huawei.hms.mlsdk.text.MLTextAnalyzer
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine
import net.maxsmr.feature.camera.recognition.ITextRecognition.RecognitionStrategy.Local
import net.maxsmr.feature.camera.recognition.ITextRecognition.RecognitionStrategy.Remote
import net.maxsmr.mobile_services.asCoroutine

class TextRecognition(apiKey: String?) : ITextRecognition {

    override val strategy: ITextRecognition.RecognitionStrategy =
        apiKey.takeIf { !it.isNullOrEmpty() }?.let { Remote(it) } ?: Local

    private val analyzer: MLTextAnalyzer by lazy {
        if (strategy is Remote) {
            val settings = MLRemoteTextSetting.Factory()
                .setTextDensityScene(MLRemoteTextSetting.OCR_COMPACT_SCENE)
                .create()
            MLAnalyzerFactory.getInstance().getRemoteTextAnalyzer(settings)
        } else {
            val settings = MLLocalTextSetting.Factory()
                .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE)
                .create()
            MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(settings)
        }
    }

    override suspend fun processFrame(frame: Image, rotationDegrees: Int): List<RecognizedLine> {
        return MLFrame.fromMediaImage(frame, getHmsQuadrant(rotationDegrees)).process()
    }

    override suspend fun processCapture(bitmap: Bitmap, rotationDegrees: Int): List<RecognizedLine> {
        return MLFrame.fromBitmap(bitmap).process()
    }

    override fun dispose() {
        analyzer.close()
    }

    private fun MLText.TextLine.toRecognizedLine(): RecognizedLine = RecognizedLine(stringValue)

    private fun getHmsQuadrant(rotationDegrees: Int): Int =
        when (rotationDegrees) {
            Surface.ROTATION_0 -> PORTRAIT_QUADRANT
            Surface.ROTATION_90 -> LANDSCAPE_QUADRANT
            Surface.ROTATION_180 -> REVERSE_PORTRAIT_QUADRANT
            Surface.ROTATION_270 -> REVERSE_LANDSCAPE_QUADRANT
            else -> PORTRAIT_QUADRANT
        }

    private suspend fun MLFrame.process(): List<RecognizedLine> {
        return analyzer.asyncAnalyseFrame(this).asCoroutine {
            it.blocks
                .flatMap { block -> block.contents }
                .map { line -> line.toRecognizedLine() }
        }
    }

    companion object {

        private const val LANDSCAPE_QUADRANT = 0
        private const val PORTRAIT_QUADRANT = 1
        private const val REVERSE_LANDSCAPE_QUADRANT = 2
        private const val REVERSE_PORTRAIT_QUADRANT = 3
    }
}