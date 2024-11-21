package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.DoubleGisRoutingHostManager
import net.maxsmr.core.di.DoubleGisRoutingOkHttpClient
import net.maxsmr.core.di.DoubleGisRoutingRetrofit
import net.maxsmr.core.di.NotificationReaderHostManager
import net.maxsmr.core.di.NotificationReaderOkHttpClient
import net.maxsmr.core.di.NotificationReaderRetrofit
import net.maxsmr.core.di.RadarIoHostManager
import net.maxsmr.core.di.RadarIoOkHttpClient
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.di.YandexGeocodeHostManager
import net.maxsmr.core.di.YandexGeocodeOkHttpClient
import net.maxsmr.core.di.YandexGeocodeRetrofit
import net.maxsmr.core.di.YandexSuggestHostManager
import net.maxsmr.core.di.YandexSuggestOkHttpClient
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.client.retrofit.YandexGeocodeRetrofitClient
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.mxstemplate.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File

@[Module
InstallIn(SingletonComponent::class)]
class RetrofitModule {

    @[Provides Singleton RadarIoRetrofit]
    fun provideRadarIoRetrofit(
        @ApplicationContext context: Context,
        @RadarIoHostManager hostManager: HostManager,
        @RadarIoOkHttpClient okHttpClient: Provider<OkHttpClient>,
        @BaseJson json: Json,
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        ) {
            okHttpClient.get()
        }
    }

    @[Provides Singleton YandexSuggestRetrofit]
    fun provideYandexSuggestRetrofit(
        @ApplicationContext context: Context,
        @YandexSuggestHostManager hostManager: HostManager,
        @YandexSuggestOkHttpClient okHttpClient: Provider<OkHttpClient>,
        @BaseJson json: Json,
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        ) {
            okHttpClient.get()
        }
    }

    @[Provides Singleton YandexGeocodeRetrofit]
    fun provideYandexGeocodeRetrofit(
        @ApplicationContext context: Context,
        @YandexGeocodeHostManager hostManager: HostManager,
        @YandexGeocodeOkHttpClient okHttpClient: Provider<OkHttpClient>,
        @BaseJson json: Json,
    ): YandexGeocodeRetrofitClient {
        return YandexGeocodeRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),

            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        ) {
            okHttpClient.get()
        }
    }

    @[Provides Singleton DoubleGisRoutingRetrofit]
    fun provideDoubleGisRoutingRetrofit(
        @ApplicationContext context: Context,
        @DoubleGisRoutingHostManager hostManager: HostManager,
        @DoubleGisRoutingOkHttpClient okHttpClient: Provider<OkHttpClient>,
        @BaseJson json: Json,
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        ) {
            okHttpClient.get()
        }
    }

    @[Provides Singleton NotificationReaderRetrofit]
    fun provideNotificationReaderRetrofit(
        @ApplicationContext context: Context,
        @NotificationReaderHostManager hostManager: HostManager,
        @NotificationReaderOkHttpClient okHttpClient: Provider<OkHttpClient>,
        @BaseJson json: Json,
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            File(context.cacheDir, "OkHttpCache").path,
            BuildConfig.PROTOCOL_VERSION,
            false
            // cacheManager.getDisableCache()
        ) {
            okHttpClient.get()
        }
    }
}