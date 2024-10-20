package net.maxsmr.feature.camera.recognition.cases

import java.io.Serializable

class GrzTextMatcherUseCase : BaseTextMatcherUseCase<GrzTextMatcherUseCase.GrzResult>() {

    override fun String.format(): String = uppercase()

    override fun String.matches(unformattedText: String, index: Int): GrzResult? {
        return GrzType.resolve(this)?.let {
            GrzResult(fixedNumber(), it)
        }
    }

    private fun String.fixedNumber(): String {
        var result = this
        if (result.isNotEmpty()) {
            for ((key, value) in rusAlphabetMap.entries) {
                result = result.replace(key, value)
            }
        }
        return result

    }

    data class GrzResult(
        val number: String,
        val type: GrzType,
    ) : Serializable

    enum class GrzType {

        RUS_STANDARD,

        RUS_TAXI,

        RUS_MOTO_TRACTOR,

        RUS_TRAILER,

        RUS_TRANSIT,

        UKR_STANDARD,

        KAZ_OLD_STANDARD,

        KAZ_NEW_STANDARD,

        BEL_STANDARD;

        fun getRegEx() = when (this) {
            RUS_STANDARD -> "^($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){1}\\d{3}($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}\\d{2,}$"
            RUS_TAXI -> "^($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}\\d{3}\\d{2,}$"
            RUS_MOTO_TRACTOR -> "^\\d{4}($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}\\d{2,}$"
            RUS_TRAILER -> "^($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}\\d{6,}$"
            RUS_TRANSIT -> "^($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}\\d{3}($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){1}\\d{2,}$"
            UKR_STANDARD -> "^($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2,}\\d{4}($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}$";
            KAZ_NEW_STANDARD -> "^\\d{3}([A-Za-z]|$GRZ_RUS_SYMBOLS){3}\\d{2,}$"
            KAZ_OLD_STANDARD -> "^\\d{3}($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}\\d{2,}$"
            BEL_STANDARD -> "^\\d{4}($GRZ_ENG_SYMBOLS|$GRZ_RUS_SYMBOLS){2}[-]{1}\\d{1,}$"
        }

        companion object {

            private const val GRZ_ENG_SYMBOLS: String = "A|B|E|K|M|H|O|P|C|T|Y|X"

            private const val GRZ_RUS_SYMBOLS: String = "А|В|Е|К|М|Н|О|Р|С|Т|У|Х"

            @JvmStatic
            fun resolve(number: String) = GrzType.entries.find { t ->
                t.getRegEx().takeIf { it.isNotEmpty() }?.let {
                    Regex(it).matches(number)
                } ?: false
            }
        }
    }

    companion object {

        private val rusAlphabetMap = mapOf(
            "E" to "Е",
            "T" to "Т",
            "Y" to "У",
            "O" to "О",
            "P" to "Р",
            "A" to "А",
            "H" to "Н",
            "K" to "К",
            "X" to "Ч",
            "C" to "С",
            "B" to "В",
            "M" to "М",
            "е" to "Е",
            "т" to "Т",
            "у" to "У",
            "о" to "О",
            "р" to "Р",
            "а" to "А",
            "н" to "Н",
            "к" to "К",
            "х" to "Х",
            "с" to "С",
            "в" to "В",
            "м" to "М",
            "e" to "Е",
            "t" to "Т",
            "y" to "У",
            "o" to "О",
            "p" to "Р",
            "a" to "А",
            "h" to "Н",
            "k" to "К",
            "x" to "Х",
            "c" to "С",
            "b" to "В",
            "m" to "М"
        )
    }
}