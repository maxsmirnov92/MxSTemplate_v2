package net.maxsmr.vk_news_client.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics.StatsType

@Parcelize
@Serializable
data class StatisticsUI(
    val type: StatsType,
    val count: Int = 0,
): Parcelable {

    companion object {

        @JvmStatic
        fun List<StatisticsUI>.getItemByType(type: StatsType): StatisticsUI = find { it.type == type}
            ?: throw IllegalArgumentException()
    }
}

fun Statistics.toStatisticsUI(): StatisticsUI {
    return StatisticsUI(type = this.type, count = this.count)
}

fun StatisticsUI.toStatistics(): Statistics {
    return Statistics(type = this.type, count = this.count)
}