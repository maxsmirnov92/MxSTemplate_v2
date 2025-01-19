package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GroupDto(
    val id: Long,
    val name: String,
    @SerialName("photo_200")
    val imageUrl: String
)