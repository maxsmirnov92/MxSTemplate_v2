package net.maxsmr.core.network.api.vk_news_client

interface BaseCommentsResponseDto {

    val comments: List<CommentDto>

    val profiles: List<ProfileDto>
}