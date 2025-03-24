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
import net.maxsmr.core.di.DownloadHttpLoggingInterceptor
import net.maxsmr.core.di.PicassoHttpLoggingInterceptor
import net.maxsmr.core.di.ResponseBodyCache
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.mxstemplate.BuildConfig
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class InterceptorModule {

    @[Provides Singleton]
    fun provideApiLoggingInterceptor(): ApiLoggingInterceptor {
        val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("ApiLoggingInterceptor")
        return ApiLoggingInterceptor { message ->
            logger.d(message)
        }.apply {
            setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
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
    fun provideBodyCachingInterceptor(
        @ResponseBodyCache cache: net.maxsmr.core.network.client.okhttp.ResponseBodyCache<Request>,
    ): BodyCachingInterceptor {
        return BodyCachingInterceptor(cache)
    }

    @[Provides Singleton]
    fun provideNetworkConnectionInterceptor(
        @ApplicationContext context: Context,
    ): NetworkConnectionInterceptor {
        return NetworkConnectionInterceptor(context, NetworkConnectivityChecker)
    }
}