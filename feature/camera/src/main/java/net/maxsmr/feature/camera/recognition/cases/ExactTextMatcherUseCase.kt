package net.maxsmr.feature.camera.recognition.cases

import net.maxsmr.commonutils.text.EMPTY_STRING

class ExactTextMatcherUseCase @JvmOverloads constructor(
    private val targetText: String,
    private val ignoreCase: Boolean = true,
) : BaseTextMatcherUseCase<String>() {

    override val garbageRegEx: String = EMPTY_STRING

    override fun String.matches(unformattedText: String, index: Int): String? =
        if (this.equals(targetText, ignoreCase)) {
            this
        } else {
            null
        }
}