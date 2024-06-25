package net.maxsmr.mobile_services.market

import net.maxsmr.commonutils.getPlayMarketIntent
import net.maxsmr.commonutils.startActivitySafe
import net.maxsmr.mobile_services.market.MarketIntentLauncher.Companion.getMarketAppId

fun startActivityMarketIntent() = MarketIntentLauncher {
    it.startActivitySafe(getPlayMarketIntent(getMarketAppId(it)))
}