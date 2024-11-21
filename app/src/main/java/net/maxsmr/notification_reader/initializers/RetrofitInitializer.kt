package net.maxsmr.notification_reader.initializers

import android.content.Context
import androidx.startup.Initializer
import net.maxsmr.core.di.NotificationReaderRetrofit
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.notification_reader.di.InitializerEntryPoint
import javax.inject.Inject

class RetrofitInitializer : Initializer<Unit> {

    @Inject
    @NotificationReaderRetrofit
    lateinit var notificationReaderRetrofit: CommonRetrofitClient

    override fun create(context: Context) {
        InitializerEntryPoint.resolve(context).inject(this)
        notificationReaderRetrofit.init()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}