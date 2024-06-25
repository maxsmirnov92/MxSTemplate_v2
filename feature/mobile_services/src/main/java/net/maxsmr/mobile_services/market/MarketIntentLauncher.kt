package net.maxsmr.mobile_services.market

import android.app.Activity
import android.content.Context

fun interface MarketIntentLauncher {

    /**
     * Запуск перехода к маркету
     */
    fun startActivityMarketIntent(activity: Activity): Boolean

    companion object {

        fun getMarketAppId(context: Context): String {
            val pkg = context.packageName
            return if (pkg.endsWith(".debug")) {
                pkg.substring(0..pkg.length - 7)
            } else {
                pkg
            }
        }
    }
}