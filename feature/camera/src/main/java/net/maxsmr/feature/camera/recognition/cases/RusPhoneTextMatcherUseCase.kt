package net.maxsmr.feature.camera.recognition.cases

import java.io.Serializable

class RusPhoneTextMatcherUseCase: BaseRegExTextMatcherUseCase<RusPhoneTextMatcherUseCase.RusPhoneResult>(
    "^\\+?\\d{11}\$"
) {

    override val garbageRegEx: String = "\\s+|[,.*\\[\\];:|\\\\/'\"_-=()]+"

    override fun String.toResult(unformattedText: String) = RusPhoneResult(this)

    data class RusPhoneResult(
        val phone: String,
    ): Serializable
}