package net.maxsmr.mobile_services.market

import android.app.Activity
import net.maxsmr.commonutils.getAnyMarketIntent
import net.maxsmr.commonutils.startActivitySafe
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileServicesAvailability

/**
 * Запуск любого маркета по урле с market://, если основной не удался
 */
class WrappedMarketIntentLauncher(
    private val marketIntentLauncher: MarketIntentLauncher,
    private val mobileServicesAvailability: IMobileServicesAvailability,
) : MarketIntentLauncher {

    override fun startActivityMarketIntent(activity: Activity): Boolean {
        var handled = false
        if (mobileServicesAvailability.isAnyServiceAvailable) {
            if (marketIntentLauncher.startActivityMarketIntent(activity)) {
                handled = true
            }
        }
        return if (!handled) {
            activity.startActivitySafe(
                getAnyMarketIntent(MarketIntentLauncher.getMarketAppId(activity))
            )
        } else {
            true
        }
    }
}