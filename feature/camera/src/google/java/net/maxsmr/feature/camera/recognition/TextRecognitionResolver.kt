package net.maxsmr.feature.camera.recognition

import net.maxsmr.mobile_services.IMobileServicesAvailability

class TextRecognitionResolver(apiKey: String, mobileServicesAvailability: IMobileServicesAvailability): BaseTextRecognitionResolver(mobileServicesAvailability) {

    override fun googleTextRecognition(): ITextRecognition = TextRecognition()

    override fun huaweiTextRecognition(): ITextRecognition {
        throw NotImplementedError("Huawei TextRecognition not available")
    }
}