package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.RadarIoHostManager
import net.maxsmr.core.di.RadarIoOkHttpClient
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.di.YandexGeocodeHostManager
import net.maxsmr.core.di.YandexGeocodeRetrofit
import net.maxsmr.core.di.YandexSuggestHostManager
import net.maxsmr.core.di.YandexOkHttpClient
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.retrofit.client.CommonRetrofitClient
import net.maxsmr.core.network.retrofit.client.YandexGeocodeRetrofitClient
import net.maxsmr.core.network.retrofit.interceptors.HostManager
import net.maxsmr.mxstemplate.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class RetrofitModule {

    @[Provides Singleton RadarIoRetrofit]
    fun provideRadarIoRetrofit(
        @ApplicationContext context: Context,
        @RadarIoHostManager hostManager: HostManager,
        @RadarIoOkHttpClient okHttpClient: OkHttpClient,
        @BaseJson json: Json,
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.getBaseUrl().toHttpUrl(),
            okHttpClient,
            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        )
    }

    @[Provides Singleton YandexSuggestRetrofit]
    fun provideYandexSuggestRetrofit(
        @ApplicationContext context: Context,
        @YandexSuggestHostManager hostManager: HostManager,
        @YandexOkHttpClient okHttpClient: OkHttpClient,
        @BaseJson json: Json,
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.getBaseUrl().toHttpUrl(),
            okHttpClient,
            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        )
    }

    @[Provides Singleton YandexGeocodeRetrofit]
    fun provideYandexGeocodeRetrofit(
        @ApplicationContext context: Context,
        @YandexGeocodeHostManager hostManager: HostManager,
        @YandexOkHttpClient okHttpClient: OkHttpClient,
        @BaseJson json: Json,
    ): YandexGeocodeRetrofitClient {
        return YandexGeocodeRetrofitClient(
            hostManager.getBaseUrl().toHttpUrl(),
            okHttpClient,
            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        )
    }
}