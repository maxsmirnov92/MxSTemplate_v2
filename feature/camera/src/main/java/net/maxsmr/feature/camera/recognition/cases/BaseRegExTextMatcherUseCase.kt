package net.maxsmr.feature.camera.recognition.cases

import java.io.Serializable

abstract class BaseRegExTextMatcherUseCase<R: Serializable>(
    vararg regExes: String,
) : BaseTextMatcherUseCase<R>() {

    private val regExes = regExes.toSet()

    final override fun String.matches(unformattedText: String, index: Int): R? {
        return if (regExes.any {
                    Regex(it).matches(this)
                }) {
            this.toResult(unformattedText)
        } else {
            null
        }
    }

    protected abstract fun String.toResult(unformattedText: String): R
}