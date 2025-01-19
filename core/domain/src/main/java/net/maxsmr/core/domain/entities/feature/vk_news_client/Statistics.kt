package net.maxsmr.core.domain.entities.feature.vk_news_client

data class Statistics(
    val type: StatsType,
    val count: Int = 0,
) {

    enum class StatsType {
        VIEWS, COMMENTS, SHARES, LIKES
    }
}