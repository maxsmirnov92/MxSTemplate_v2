package net.maxsmr.notification_reader.di.modules

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
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.di.NotificationReaderHostManager
import net.maxsmr.core.di.NotificationReaderOkHttpClient
import net.maxsmr.core.network.client.okhttp.DownloadOkHttpClientManager
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.di.holder.NotificationReaderHostManagerHolder
import net.maxsmr.notification_reader.di.holder.NotificationReaderOkHttpClientHolder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class OkHttpModule {

    @[Provides Singleton DownloadHttpLoggingInterceptor]
    fun provideDownloadHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("DownloadHttpLoggingInterceptor")
        return HttpLoggingInterceptor { message -> logger.d("OkHttp $message") }.apply {
            // логирование body при тяжёлых ответах будет приводить к OOM
            level = HttpLoggingInterceptor.Level.HEADERS
        }
    }

    @[Provides Singleton DownloaderOkHttpClient]
    fun provideDownloaderOkHttpClient(
        @ApplicationContext context: Context,
        @DownloadHttpLoggingInterceptor
        httpLoggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = DownloadOkHttpClientManager(
        context,
        NetworkConnectivityChecker,
        httpLoggingInterceptor,
    ).build()

    @[Provides Singleton NotificationReaderOkHttpClient]
    fun provideNotificationReaderOkHttpClient(
        @ApplicationContext context: Context,
        settingsRepository: SettingsDataStoreRepository,
        @NotificationReaderHostManager
        hostManager: NotificationReaderHostManagerHolder,
    ): NotificationReaderOkHttpClientHolder {
        return NotificationReaderOkHttpClientHolder(settingsRepository, hostManager, context)
    }
}