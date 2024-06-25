package net.maxsmr.mobile_services.market

import net.maxsmr.commonutils.getHuaweiMarketIntent
import net.maxsmr.commonutils.startActivitySafe
import net.maxsmr.mobile_services.market.MarketIntentLauncher.Companion.getMarketAppId

/**
 * Ничего не делать, если соотв. сборке маркет не был обнаружен
 */
fun startActivityMarketIntent() = MarketIntentLauncher {
    it.startActivitySafe(getHuaweiMarketIntent(getMarketAppId(it)))
}