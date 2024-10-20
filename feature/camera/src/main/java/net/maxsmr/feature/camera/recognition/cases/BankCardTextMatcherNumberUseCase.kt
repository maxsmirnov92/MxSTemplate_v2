package net.maxsmr.feature.camera.recognition.cases

import java.io.Serializable

class BankCardTextMatcherNumberUseCase : BaseRegExTextMatcherUseCase<BankCardTextMatcherNumberUseCase.CardNumberResult>("^\\d{16}$") {

    override fun String.toResult(unformattedText: String) = CardNumberResult(this)

    data class CardNumberResult(
        val number: String,
    ) : Serializable
}