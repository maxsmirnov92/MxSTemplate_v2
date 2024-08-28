package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.network.NetworkConnectivityChecker
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.di.DoubleGisRoutingOkHttpClient
import net.maxsmr.core.di.DownloadHttpLoggingInterceptor
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.di.PicassoHttpLoggingInterceptor
import net.maxsmr.core.di.PicassoOkHttpClient
import net.maxsmr.core.di.RadarIoOkHttpClient
import net.maxsmr.core.di.YandexGeocodeOkHttpClient
import net.maxsmr.core.di.YandexSuggestOkHttpClient
import net.maxsmr.core.network.client.okhttp.DoubleGisOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.DownloadOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.PicassoOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.RadarIoOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.YandexOkHttpClientManager
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import net.maxsmr.core.network.retrofit.converters.api.BaseDoubleGisRoutingResponse
import net.maxsmr.core.network.retrofit.converters.api.BaseYandexSuggestResponse
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.di.ModuleAppEntryPoint
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import javax.inject.Singleton

private const val NETWORK_TIMEOUT = 30L //sec

// Размер дискового кеша пикассо = 250 Мб
private const val PICASSO_DISK_CACHE_SIZE = (1024 * 1024 * 250).toLong()
private const val PICASSO_CACHE = "picasso-cache"

@[Module
InstallIn(SingletonComponent::class)]
class OkHttpModule {

    @[Provides Singleton]
    fun provideForceCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val builder = chain.request().newBuilder()
            if (!NetworkStateManager.hasConnection()) {
                builder.cacheControl(CacheControl.FORCE_CACHE)
            }
            chain.proceed(builder.build())
        }
    }

    @[Provides Singleton DownloadHttpLoggingInterceptor]
    fun provideDownloadHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("DownloadHttpLoggingInterceptor")
        return HttpLoggingInterceptor { message -> logger.d("OkHttp $message") }.apply {
            // логирование body при тяжёлых ответах будет приводить к OOM
            level = HttpLoggingInterceptor.Level.HEADERS
        }
    }

    @[Provides Singleton PicassoHttpLoggingInterceptor]
    fun providePicassoHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("CommonHttpLoggingInterceptor")
        return HttpLoggingInterceptor { message -> logger.d("OkHttp $message") }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.HEADERS
            }
        }
    }

    @[Provides Singleton PicassoOkHttpClient]
    fun providePicassoOkHttpClient(
        @ApplicationContext context: Context,
        forceCacheInterceptor: Interceptor,
        @PicassoHttpLoggingInterceptor
        httpLoggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        // Каталог кэша Picasso
        val cacheDir = File(context.cacheDir, PICASSO_CACHE)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return PicassoOkHttpClientManager(
            forceCacheInterceptor,
            httpLoggingInterceptor,
            NETWORK_TIMEOUT
        ).build()
    }

    @[Provides Singleton DownloaderOkHttpClient]
    fun provideDownloaderOkHttpClient(
        @DownloadHttpLoggingInterceptor
        httpLoggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = DownloadOkHttpClientManager(
        NetworkConnectivityChecker,
        httpLoggingInterceptor,
        NETWORK_TIMEOUT
    ).build()

    @[Provides Singleton RadarIoOkHttpClient]
    fun provideRadarIoOkHttpClient(): OkHttpClient {
        return RadarIoOkHttpClientManager(
            BuildConfig.AUTHORIZATION_RADAR_IO,
            connectivityChecker = NetworkConnectivityChecker,
            callTimeout = NETWORK_TIMEOUT
        ) {
            EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java)
                .radarIoRetrofit().instance
        }.build()
    }

    @[Provides Singleton YandexSuggestOkHttpClient]
    fun provideYandexSuggestOkHttpClient(): OkHttpClient {
        return YandexOkHttpClientManager(
            BuildConfig.API_KEY_YANDEX_SUGGEST,
            YandexOkHttpClientManager.LocalizationField.LANG,
            "ru",
            NetworkConnectivityChecker,
            NETWORK_TIMEOUT,
            responseAnnotation = ResponseObjectType(BaseYandexSuggestResponse::class),
        ) {
            EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java)
                .yandexSuggestRetrofit().instance
        }.build()
    }

    @[Provides Singleton YandexGeocodeOkHttpClient]
    fun provideYandexGeocodeOkHttpClient(): OkHttpClient {
        return YandexOkHttpClientManager(
            BuildConfig.API_KEY_YANDEX_GEOCODE,
            YandexOkHttpClientManager.LocalizationField.LOCALE,
            "ru_RU",
            NetworkConnectivityChecker,
            NETWORK_TIMEOUT,
            responseAnnotation = null // ситуативный BaseEnvelopeWithObject подставить нельзя, а BaseEnvelope не требуется
        ) {
            EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java)
                .yandexGeocodeRetrofit().instance
        }.build()
    }

    @[Provides Singleton DoubleGisRoutingOkHttpClient]
    fun provideDoubleGisRoutingOkHttpClient(): OkHttpClient {
        return DoubleGisOkHttpClientManager(
            BuildConfig.DEMO_KEY_DOUBLE_GIS_ROUTING,
            connectivityChecker = NetworkConnectivityChecker,
            callTimeout = NETWORK_TIMEOUT
        ) {
            EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java)
                .doubleGisRoutingRetrofit().instance
        }.build()
    }
}