package net.maxsmr.feature.camera.recognition

import net.maxsmr.mobile_services.IMobileServicesAvailability

class TextRecognitionResolver(
    private val apiKey: String?,
    mobileServicesAvailability: IMobileServicesAvailability
): BaseTextRecognitionResolver(mobileServicesAvailability) {

    override fun googleTextRecognition(): ITextRecognition {
        throw NotImplementedError("Huawei TextRecognition not available")
    }

    override fun huaweiTextRecognition(): ITextRecognition = TextRecognition(apiKey)
}