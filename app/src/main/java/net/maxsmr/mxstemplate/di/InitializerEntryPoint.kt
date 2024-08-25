package net.maxsmr.mxstemplate.di

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.YandexGeocodeRetrofit
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.retrofit.client.CommonRetrofitClient
import net.maxsmr.core.network.retrofit.client.YandexGeocodeRetrofitClient
import net.maxsmr.mxstemplate.initializers.RetrofitInitializer

@[EntryPoint
InstallIn(SingletonComponent::class)]
interface InitializerEntryPoint {

    fun inject(initializer: RetrofitInitializer)

    companion object {

        fun resolve(context: Context): InitializerEntryPoint {
            val appContext = context.applicationContext ?: throw IllegalStateException()
            return EntryPointAccessors.fromApplication(
                appContext,
                InitializerEntryPoint::class.java
            )
        }
    }
}