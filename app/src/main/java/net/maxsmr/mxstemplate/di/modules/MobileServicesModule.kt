package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.android.location.receiver.ILocationReceiver
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileServicesAvailability
import net.maxsmr.mobile_services.market.MarketIntentLauncher
import net.maxsmr.mobile_services.market.WrappedMarketIntentLauncher
import net.maxsmr.mobile_services.market.startActivityMarketIntent
import net.maxsmr.mobile_services.receiver.LocationReceiverResolver
import javax.inject.Singleton

/**
 * Зависимости от выбранного флейвора (google, huawei)
 */
@[Module
InstallIn(SingletonComponent::class)]
class MobileServicesModule {

    /**
     * Интент перехода к маркету
     */
    @[Singleton Provides]
    fun provideMarketIntent(mobileServicesAvailability: IMobileServicesAvailability): MarketIntentLauncher =
        WrappedMarketIntentLauncher(startActivityMarketIntent(), mobileServicesAvailability)

    /**
     * Доступность сервисов HMS/GMS **на устройстве**
     */
    @[Singleton Provides]
    fun provideMobileServiceAvailability(
        @ApplicationContext context: Context,
    ): IMobileServicesAvailability = MobileServicesAvailability(context)


    @[Singleton Provides]
    fun provideLocationReceiver(
        @ApplicationContext context: Context,
        availability: IMobileServicesAvailability,
    ): ILocationReceiver {
        return LocationReceiverResolver(context, availability).resolve()
    }
}