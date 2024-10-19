package net.maxsmr.core.domain.entities.feature.recognition

data class RecognizedLine(val text: String) {

    companion object {

        @JvmStatic
        fun Collection<RecognizedLine>.joinLines() = joinToString("\n") { it.text }
    }
}