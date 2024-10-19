package net.maxsmr.feature.camera.recognition

import net.maxsmr.mobile_services.IMobileServicesAvailability

abstract class BaseTextRecognitionResolver(
    private val mobileServicesAvailability: IMobileServicesAvailability
) {

    protected abstract fun googleTextRecognition(): ITextRecognition

    protected abstract fun huaweiTextRecognition(): ITextRecognition

    fun resolve(): ITextRecognition {
        return when {
            mobileServicesAvailability.isGooglePlayServicesAvailable -> googleTextRecognition()
            mobileServicesAvailability.isHuaweiApiServicesAvailable -> huaweiTextRecognition()
            else -> throw RuntimeException("Google/Huawei services not available")
        }
    }

}