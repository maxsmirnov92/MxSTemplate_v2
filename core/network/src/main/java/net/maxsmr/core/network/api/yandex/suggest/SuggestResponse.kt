package net.maxsmr.core.network.api.yandex.suggest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.retrofit.converters.api.BaseYandexSuggestResponse

@Serializable
class SuggestResponse(
    val results: List<Result> = emptyList(),
) : BaseYandexSuggestResponse() {

    fun asDomain(): List<AddressSuggest> = results.map { it.asDomain() }.filter { it.displayedAddress.isNotEmpty() }

    @Serializable
    class Result(
        val title: Title? = null,
        val subtitle: Subtitle? = null,
        val tags: List<String> = emptyList(),
        val distance: Distance? = null,
        val address: Address? = null,
        val uri: String? = null,
    ) {

        fun asDomain(): AddressSuggest {
            val formattedAddress = address?.formattedAddress?.takeIf { it.isNotEmpty() }
            val title = title?.text.orEmpty()
            val subtitle = subtitle?.text?.takeIf { it.isNotEmpty() }
            return AddressSuggest(
                formattedAddress ?: (subtitle?.let {
                    if (title.isNotEmpty()) {
                        "$it, $title"
                    } else {
                        title
                    }
                } ?: title),
                formattedAddress ?: subtitle ?: title,
                null,
                distance?.value?.takeIf { it >= 0 }
            )
        }

        @Serializable
        class Title(
            val text: String,
            val hl: List<Hl> = emptyList(),
        ) {

            @Serializable
            class Hl(
                val begin: Int,
                val end: Int,
            )
        }

        @Serializable
        class Subtitle(
            val text: String,
        )

        @Serializable
        class Distance(
            val text: String,
            val value: Float,
        )

        @Serializable
        class Address(
            @SerialName("formatted_address")
            val formattedAddress: String,
        )
    }
}