package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.Serializable

@Serializable
data class ResponseStatusDto(
    val status: Boolean
)