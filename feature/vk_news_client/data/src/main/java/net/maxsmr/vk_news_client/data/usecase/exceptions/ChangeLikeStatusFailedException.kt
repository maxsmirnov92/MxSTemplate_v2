package net.maxsmr.vk_news_client.data.usecase.exceptions

class ChangeLikeStatusFailedException(val isAddLike: Boolean): RuntimeException()