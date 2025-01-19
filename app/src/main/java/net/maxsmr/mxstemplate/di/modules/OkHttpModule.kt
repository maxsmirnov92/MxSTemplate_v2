package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.di.DoubleGisRoutingOkHttpClient
import net.maxsmr.core.di.DownloadHttpLoggingInterceptor
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.di.PicassoHttpLoggingInterceptor
import net.maxsmr.core.di.PicassoOkHttpClient
import net.maxsmr.core.di.RadarIoOkHttpClient
import net.maxsmr.core.di.ResponseBodyCache
import net.maxsmr.core.di.SessionStorageType
import net.maxsmr.core.di.VkOkHttpClient
import net.maxsmr.core.di.YandexGeocodeOkHttpClient
import net.maxsmr.core.di.YandexSuggestOkHttpClient
import net.maxsmr.core.network.client.okhttp.DoubleGisOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.DownloadOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.PicassoOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.RadarIoOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.VkOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.YandexOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.core.network.session.SessionStorage
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mxstemplate.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import javax.inject.Singleton

// Размер дискового кеша пикассо = 250 Мб
//private const val PICASSO_DISK_CACHE_SIZE = (1024 * 1024 * 250).toLong()
private const val PICASSO_CACHE = "picasso-cache"

@[Module
InstallIn(SingletonComponent::class)]
class OkHttpModule {

    @[Provides Singleton PicassoOkHttpClient]
    fun providePicassoOkHttpClient(
        @PicassoHttpLoggingInterceptor
        httpLoggingInterceptor: HttpLoggingInterceptor,
        @ApplicationContext context: Context,
        forceCacheInterceptor: Interceptor,
    ): OkHttpClient {
        // Каталог кэша Picasso
        val cacheDir = File(context.cacheDir, PICASSO_CACHE)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return PicassoOkHttpClientManager(
            httpLoggingInterceptor,
            forceCacheInterceptor,
        ).build()
    }

    @[Provides Singleton DownloaderOkHttpClient]
    fun provideDownloaderOkHttpClient(
        @DownloadHttpLoggingInterceptor
        httpLoggingInterceptor: HttpLoggingInterceptor,
        connectionInterceptor: NetworkConnectionInterceptor,
    ): OkHttpClient = DownloadOkHttpClientManager(
        httpLoggingInterceptor,
        connectionInterceptor,
    ).build()

    @[Provides Singleton RadarIoOkHttpClient]
    fun provideRadarIoOkHttpClient(
        apiLoggingInterceptor: ApiLoggingInterceptor,
        cachingInterceptor: BodyCachingInterceptor,
        connectionInterceptor: NetworkConnectionInterceptor,
    ): OkHttpClient {
        return RadarIoOkHttpClientManager(
            BuildConfig.AUTHORIZATION_RADAR_IO,
            apiLoggingInterceptor = apiLoggingInterceptor,
            cachingInterceptor = cachingInterceptor,
            connectionInterceptor = connectionInterceptor
        ).build()
    }

    @[Provides Singleton YandexSuggestOkHttpClient]
    fun provideYandexSuggestOkHttpClient(
        apiLoggingInterceptor: ApiLoggingInterceptor,
        cachingInterceptor: BodyCachingInterceptor,
        connectionInterceptor: NetworkConnectionInterceptor,
    ): OkHttpClient {
        return YandexOkHttpClientManager(
            BuildConfig.API_KEY_YANDEX_SUGGEST,
            YandexOkHttpClientManager.LocalizationField.LANG,
            "ru",
            apiLoggingInterceptor = apiLoggingInterceptor,
            cachingInterceptor = cachingInterceptor,
            connectionInterceptor = connectionInterceptor
        ).build()
    }

    @[Provides Singleton YandexGeocodeOkHttpClient]
    fun provideYandexGeocodeOkHttpClient(
        apiLoggingInterceptor: ApiLoggingInterceptor,
        cachingInterceptor: BodyCachingInterceptor,
        connectionInterceptor: NetworkConnectionInterceptor,
    ): OkHttpClient {
        return YandexOkHttpClientManager(
            BuildConfig.API_KEY_YANDEX_GEOCODE,
            YandexOkHttpClientManager.LocalizationField.LOCALE,
            "ru_RU",
            apiLoggingInterceptor = apiLoggingInterceptor,
            cachingInterceptor = cachingInterceptor,
            connectionInterceptor = connectionInterceptor
        ).build()
    }

    @[Provides Singleton DoubleGisRoutingOkHttpClient]
    fun provideDoubleGisRoutingOkHttpClient(
        apiLoggingInterceptor: ApiLoggingInterceptor,
        cachingInterceptor: BodyCachingInterceptor,
        connectionInterceptor: NetworkConnectionInterceptor,
        cacheRepo: CacheDataStoreRepository,
    ): OkHttpClient {
        return DoubleGisOkHttpClientManager(
            apiLoggingInterceptor = apiLoggingInterceptor,
            cachingInterceptor = cachingInterceptor,
            connectionInterceptor = connectionInterceptor,
            apiKeyProvider = {
                runBlocking { cacheRepo.getDoubleGisRoutingApiKey() }
            }
        ).build()
    }

    @[Provides Singleton VkOkHttpClient]
    fun provideVkOkHttpClient(
        @net.maxsmr.core.di.SessionStorage(SessionStorageType.VK) sessionStorage: SessionStorage,
        apiLoggingInterceptor: ApiLoggingInterceptor,
        cachingInterceptor: BodyCachingInterceptor,
        connectionInterceptor: NetworkConnectionInterceptor,
        cacheRepo: CacheDataStoreRepository,
    ): OkHttpClient {
        return VkOkHttpClientManager(
            apiLoggingInterceptor = apiLoggingInterceptor,
            cachingInterceptor = cachingInterceptor,
            connectionInterceptor = connectionInterceptor,
            sessionStorage = sessionStorage,
        ).build()
    }

    @[Provides Singleton ResponseBodyCache]
    fun providesResponseBodyCache(): net.maxsmr.core.network.client.okhttp.ResponseBodyCache<Request> {
        return net.maxsmr.core.network.client.okhttp.ResponseBodyCache {
            this
        }
    }
}