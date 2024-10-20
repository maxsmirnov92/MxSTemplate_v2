package net.maxsmr.feature.camera.recognition.cases

class CommonRegExTextMatcherUseCase(regExes: Collection<String>): BaseRegExTextMatcherUseCase<String>(*regExes.toTypedArray()) {

    override fun String.toResult(unformattedText: String): String = this
}