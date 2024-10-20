package net.maxsmr.feature.camera.recognition.cases

import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine
import net.maxsmr.core.domain.entities.feature.recognition.RecognizedLine.Companion.joinLines
import java.io.Serializable

abstract class BaseTextMatcherUseCase<R : Serializable> : UseCase<List<RecognizedLine>, BaseTextMatcherUseCase.RecognizedResult<R>>(Dispatchers.Default) {

    protected open val garbageRegEx: String = REG_EX_GARBAGE

    final override suspend fun execute(parameters: List<RecognizedLine>): RecognizedResult<R> {
        var result: R? = null
        parameters.map { it.text }.forEachIndexed { i, item ->
            val formattedText = item.format()
            formattedText.matches(item, i)?.let { r ->
                result = r
                return@forEachIndexed
            }
        }
        val source = parameters.joinLines()
        if (result == null) {
            throw FailedRecognitionException(source)
        }
        return RecognizedResult(result!!, source)
    }

    abstract fun String.matches(unformattedText: String, index: Int): R?

    protected open fun String.format() : String {
        return if (garbageRegEx.isEmpty()) {
            this
        } else {
            this.replace(Regex(garbageRegEx), "")
        }
    }

    data class RecognizedResult<R : Serializable>(
        val result: R,
        val sourceText: String,
    )

    class FailedRecognitionException(val sourceText: String) : RuntimeException()

    companion object {

        private const val REG_EX_GARBAGE = "\\s+|[,.*\\[\\];:|\\\\/'\"_-—+=|·()]+"
    }
}