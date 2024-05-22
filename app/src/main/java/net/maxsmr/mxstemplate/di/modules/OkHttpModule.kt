package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.network.NetworkConnectivityChecker
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.di.PicassoOkHttpClient
import net.maxsmr.core.di.RadarIoOkHttpClient
import net.maxsmr.core.di.RadarIoSessionStorage
import net.maxsmr.core.network.SessionStorage
import net.maxsmr.core.network.retrofit.client.okhttp.DownloadOkHttpClientManager
import net.maxsmr.core.network.retrofit.client.okhttp.PicassoOkHttpClientManager
import net.maxsmr.core.network.retrofit.client.okhttp.RadarIoOkHttpClientManager
import net.maxsmr.core.network.retrofit.interceptors.NetworkConnectionInterceptor
import net.maxsmr.mxstemplate.BuildConfig
import okhttp3.Cache
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

    @[Provides Singleton]
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("HttpLoggingInterceptor")
        return HttpLoggingInterceptor { message -> logger.d("OkHttp $message") }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    @[Provides Singleton PicassoOkHttpClient]
    fun providePicassoOkHttpClient(
        @ApplicationContext context: Context,
        forceCacheInterceptor: Interceptor,
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
        httpLoggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = DownloadOkHttpClientManager(
        NetworkConnectionInterceptor(NetworkConnectivityChecker),
        httpLoggingInterceptor,
        NETWORK_TIMEOUT
    ).build()

    @[Provides Singleton RadarIoOkHttpClient]
    fun provideRadarIoOkHttpClient(
        @RadarIoSessionStorage
        sessionStorage: SessionStorage
    ): OkHttpClient {
        return RadarIoOkHttpClientManager(
            NetworkConnectivityChecker,
            sessionStorage,
            NETWORK_TIMEOUT
        ).build()
    }
}