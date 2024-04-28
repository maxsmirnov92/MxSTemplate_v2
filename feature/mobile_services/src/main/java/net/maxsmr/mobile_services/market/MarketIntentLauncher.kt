package net.maxsmr.mobile_services.market

import android.app.Activity

fun interface MarketIntentLauncher {

    /**
     * Запуск перехода к маркету
     */
    fun startActivityMarketIntent(activity: Activity): Unit
}