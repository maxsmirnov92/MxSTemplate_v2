package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.DoubleGisRoutingHostManager
import net.maxsmr.core.di.DoubleGisRoutingOkHttpClient
import net.maxsmr.core.di.DoubleGisRoutingRetrofit
import net.maxsmr.core.di.RadarIoHostManager
import net.maxsmr.core.di.RadarIoOkHttpClient
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.di.ResponseBodyCache
import net.maxsmr.core.di.YandexGeocodeHostManager
import net.maxsmr.core.di.YandexGeocodeOkHttpClient
import net.maxsmr.core.di.YandexGeocodeRetrofit
import net.maxsmr.core.di.YandexSuggestHostManager
import net.maxsmr.core.di.YandexSuggestOkHttpClient
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.client.retrofit.RetrofitClient
import net.maxsmr.core.network.client.retrofit.YandexGeocodeRetrofitClient
import net.maxsmr.core.network.exceptions.handler.CombinedCallExceptionHandler
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.manager.CacheManager
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class RetrofitModule {

    @[Provides Singleton RadarIoRetrofit]
    fun provideRadarIoRetrofit(
        cacheManager: CacheManager,
        exceptionHandler: CombinedCallExceptionHandler,
        @RadarIoHostManager hostManager: HostManager,
        @RadarIoOkHttpClient okHttpClient: OkHttpClient,
        @BaseJson json: Json,
        @ResponseBodyCache cache: net.maxsmr.core.network.client.okhttp.ResponseBodyCache<Request>
    ): RetrofitClient {
        return RetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            cacheManager.dirPath,
            BuildConfig.PROTOCOL_VERSION,
            cacheManager.disableCache,
            cache,
            exceptionHandler,
        ) {
            okHttpClient
        }
    }

    @[Provides Singleton YandexSuggestRetrofit]
    fun provideYandexSuggestRetrofit(
        cacheManager: CacheManager,
        exceptionHandler: CombinedCallExceptionHandler,
        @YandexSuggestHostManager hostManager: HostManager,
        @YandexSuggestOkHttpClient okHttpClient: OkHttpClient,
        @BaseJson json: Json,
        @ResponseBodyCache cache: net.maxsmr.core.network.client.okhttp.ResponseBodyCache<Request>
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            cacheManager.dirPath,
            BuildConfig.PROTOCOL_VERSION,
            cacheManager.disableCache,
            cache,
            exceptionHandler
        ) {
            okHttpClient
        }
    }

    @[Provides Singleton YandexGeocodeRetrofit]
    fun provideYandexGeocodeRetrofit(
        cacheManager: CacheManager,
        exceptionHandler: CombinedCallExceptionHandler,
        @YandexGeocodeHostManager hostManager: HostManager,
        @YandexGeocodeOkHttpClient okHttpClient: OkHttpClient,
        @BaseJson json: Json,
        @ResponseBodyCache cache: net.maxsmr.core.network.client.okhttp.ResponseBodyCache<Request>
    ): YandexGeocodeRetrofitClient {
        return YandexGeocodeRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            cacheManager.dirPath,
            BuildConfig.PROTOCOL_VERSION,
            cacheManager.disableCache,
            cache,
            exceptionHandler
        ) {
            okHttpClient
        }
    }

    @[Provides Singleton DoubleGisRoutingRetrofit]
    fun provideDoubleGisRoutingRetrofit(
        cacheManager: CacheManager,
        exceptionHandler: CombinedCallExceptionHandler,
        @DoubleGisRoutingHostManager hostManager: HostManager,
        @DoubleGisRoutingOkHttpClient okHttpClient: OkHttpClient,
        @BaseJson json: Json,
        @ResponseBodyCache cache: net.maxsmr.core.network.client.okhttp.ResponseBodyCache<Request>
    ): CommonRetrofitClient {
        return CommonRetrofitClient(
            hostManager.baseUrl.toHttpUrl(),
            json,
            cacheManager.dirPath,
            BuildConfig.PROTOCOL_VERSION,
            cacheManager.disableCache,
            cache,
            exceptionHandler
        ) {
            okHttpClient
        }
    }

    @[Provides Singleton]
    fun provideApiExceptionHandler(): CombinedCallExceptionHandler {
        return CombinedCallExceptionHandler(listOf())
    }
}