package net.maxsmr.mobile_services.market

import net.maxsmr.commonutils.getPlayMarketIntent
import net.maxsmr.commonutils.startActivitySafe

/**
 * Ничего не делать, если соотв. сборке маркет не был обнаружен
 */
fun startActivityMarketIntent() = MarketIntentLauncher {
    val pkg = it.packageName
    val id = if (pkg.endsWith(".debug")) pkg.substring(0..pkg.length - 7) else pkg
    it.startActivitySafe(getPlayMarketIntent(id))
}