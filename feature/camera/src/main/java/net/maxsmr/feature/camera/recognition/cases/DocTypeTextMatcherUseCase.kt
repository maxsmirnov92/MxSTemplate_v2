package net.maxsmr.feature.camera.recognition.cases

import java.io.Serializable

class DocTypeTextMatcherUseCase: BaseTextMatcherUseCase<DocTypeTextMatcherUseCase.DocumentResult>() {

    override fun String.matches(unformattedText: String, index: Int): DocumentResult? {
        val type = DocumentType.resolve(this)
        return type?.let {
            DocumentResult(this, it)
        }
    }

    data class DocumentResult(
        val number: String,
        val type: DocumentType
    ): Serializable

    /**
     * @param validityRegExp Регулярка для номера документа
     * @param minLettersCount Дополнительная проверка на наличие минимального кол-ва букв в номере
     * @param minDigitsCount Дополнительная проверка на наличие минимального кол-ва цифр в номере
     */
    enum class DocumentType(
        val validityRegExp: String,
        val minLettersCount: Int = 0,
        val minDigitsCount: Int = 0,
    ) {

        PASSPORT_RF(
            "^\\d{10}$",
        ),

        /**
         * Заграничный паспорт
         */
        INTERNATIONAL_PASSPORT(
            "^\\d{9}$",
        ),

        /**
         * Свидетельство о рождении
         */
        BIRTH_CERT(
            "^[XVIxvi]{1,6}[А-Яа-яёЁ]{2}\\d{6}$",
        ),

        /**
         * Документ, выданный другим государством (кроме РФ)
         */
//        FOREIGN_DOC(
//            "^([a-zA-Z]{5,16})|(?=.*\\d)([a-zA-Zа-яА-ЯёЁ0-9]{1,16})$",
//            minDigitsCount = 1
//        ),

        /**
         * Вид на жительство в РФ
         */
        REGISTRATION_DOC(
            "^\\d{9}\$",
        ),

        /**
         * Военный билет в/сл срочной службы, по контракту и курсантов
         */
        MILITARY_ID_1(
            "[А-Яа-я]{2}\\d{7}$",
        ),

        /**
         * Медицинское свидетельство о рождении
         */
//        MEDICAL_BIRTH_CERT(
//            "^\\d{6,12}$",
//        ),

        
        /**
         * Паспорт формы СССР
         */
        PASSPORT_USSR(
            "^[XVIxvi]{1,6}[А-Яа-яёЁ]{2}\\d{6}$",
        ),
        
        /**
         * Удостоверение личности моряка
         */
        SEAMAN_ID(
            "^[\\d]{7}$",
        );

        /**
         * Прочие типы документов
         */
//        OTHER(
//            "^.*\\d.*$"
//        );

        fun isValid(documentNumber: String?): Boolean {
            if (validityRegExp.takeIf { it.isNotEmpty() }?.let {
                        documentNumber?.matches((it).toRegex()) == true
                    } == false) {
                return false
            }
            if (minDigitsCount > 0 || minLettersCount > 0) {
                var digitsCount = 0
                var lettersCount = 0
                documentNumber?.toCharArray()?.forEach {
                    when {
                        Character.isDigit(it) -> digitsCount++
                        Character.isLetter(it) -> lettersCount++
                    }
                }
                if (minDigitsCount > 0 && digitsCount < minDigitsCount
                        || minLettersCount > 0 && lettersCount < minLettersCount
                ) {
                    return false
                }
            }
            return true
        }

        companion object {

            @JvmStatic
            fun resolve(number: String) = DocumentType.entries.find {
                it.isValid(number)
            }
        }
    }
}