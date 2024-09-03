package net.maxsmr.address_sorter.initializers

import android.content.Context
import androidx.startup.Initializer
import net.maxsmr.address_sorter.di.InitializerEntryPoint
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.di.DoubleGisRoutingRetrofit
import net.maxsmr.core.di.YandexGeocodeRetrofit
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.client.retrofit.YandexGeocodeRetrofitClient
import javax.inject.Inject

class RetrofitInitializer : Initializer<Unit> {

//    @Inject
//    @RadarIoRetrofit
//    lateinit var radarIoRetrofit: CommonRetrofitClient

    @Inject
    @YandexSuggestRetrofit
    lateinit var yandexSuggestRetrofit: CommonRetrofitClient

    @Inject
    @YandexGeocodeRetrofit
    lateinit var geocodeRetrofitClient: YandexGeocodeRetrofitClient

    @Inject
    @DoubleGisRoutingRetrofit
    lateinit var doubleGisRoutingRetrofit: CommonRetrofitClient

    override fun create(context: Context) {
        InitializerEntryPoint.resolve(context).inject(this)
//        radarIoRetrofit.init()
        yandexSuggestRetrofit.init()
        geocodeRetrofitClient.init()
        doubleGisRoutingRetrofit.init()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}