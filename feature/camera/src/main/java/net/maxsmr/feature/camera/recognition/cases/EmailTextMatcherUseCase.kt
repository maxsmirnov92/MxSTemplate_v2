package net.maxsmr.feature.camera.recognition.cases

import java.io.Serializable

class EmailTextMatcherUseCase: BaseRegExTextMatcherUseCase<EmailTextMatcherUseCase.EmailResult>(
    "^.+@.+\\..+$"
) {

    override val garbageRegEx: String = super.garbageRegEx.replace(Regex("[._-]"), "")

    override fun String.toResult(unformattedText: String) = EmailResult(this)

    data class EmailResult(
        val email: String,
    ): Serializable
}