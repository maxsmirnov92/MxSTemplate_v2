package net.maxsmr.justupdownloadit.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.DownloadHttpLoggingInterceptor
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.network.client.okhttp.DownloadOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class OkHttpModule {

    @[Provides Singleton DownloaderOkHttpClient]
    fun provideDownloaderOkHttpClient(
        @DownloadHttpLoggingInterceptor
        httpLoggingInterceptor: HttpLoggingInterceptor,
        connectionInterceptor: NetworkConnectionInterceptor,
    ): OkHttpClient = DownloadOkHttpClientManager(
        httpLoggingInterceptor,
        connectionInterceptor,
    ).build()
}