package net.maxsmr.justupdownloadit.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.network.NetworkConnectivityChecker
import net.maxsmr.core.di.DownloadHttpLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class InterceptorModule {

    @[Provides Singleton DownloadHttpLoggingInterceptor]
    fun provideDownloadHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("DownloadHttpLoggingInterceptor")
        return HttpLoggingInterceptor { message -> logger.d("OkHttp $message") }.apply {
            // логирование body при тяжёлых ответах будет приводить к OOM
            level = HttpLoggingInterceptor.Level.HEADERS
        }
    }

    @[Provides Singleton]
    fun provideNetworkConnectionInterceptor(
        @ApplicationContext context: Context,
        connectivityChecker: NetworkConnectivityChecker
    ): NetworkConnectionInterceptor {
        return NetworkConnectionInterceptor(context, connectivityChecker)
    }
}