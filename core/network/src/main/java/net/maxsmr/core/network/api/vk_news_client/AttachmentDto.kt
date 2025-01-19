package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentDto(
    val type: String,
    val photo: Photo? = null,
    val video: Video? = null,
) {

    @Serializable
    data class Photo(@SerialName("sizes") val images: List<ImageDto> = emptyList())

    @Serializable
    data class Video(@SerialName("image") val images: List<ImageDto> = emptyList())
}