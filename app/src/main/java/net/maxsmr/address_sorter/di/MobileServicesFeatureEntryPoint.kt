package net.maxsmr.address_sorter.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.android.location.receiver.ILocationReceiver
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.market.MarketIntentLauncher

/**
 * Инициализация зависимостей HMS/GMS
 */
@[EntryPoint
InstallIn(SingletonComponent::class)]
interface MobileServicesFeatureEntryPoint {

    /**
     * [Intent] для перехода к маркету
     */
    val marketIntentLauncher: MarketIntentLauncher

    /**
     * Доступность сервисов HMS/GMS **на устройстве**
     */
    val mobileServicesAvailability: IMobileServicesAvailability

    val locationReceiver: ILocationReceiver
}