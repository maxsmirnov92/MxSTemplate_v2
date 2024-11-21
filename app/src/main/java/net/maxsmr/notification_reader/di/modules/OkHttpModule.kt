package net.maxsmr.notification_reader.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.network.NetworkConnectivityChecker
import net.maxsmr.core.di.DownloadHttpLoggingInterceptor
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.di.NotificationReaderHostManager
import net.maxsmr.core.di.NotificationReaderOkHttpClient
import net.maxsmr.core.network.client.okhttp.DownloadOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.NotificationReaderOkHttpClientManager
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.BuildConfig
import net.maxsmr.notification_reader.di.ModuleAppEntryPoint
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Provider
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
        cacheRepo: CacheDataStoreRepository,
        settingsRepository: SettingsDataStoreRepository,
        @NotificationReaderHostManager hostManager: Provider<HostManager>,
    ): OkHttpClient {
        return runBlocking {
            NotificationReaderOkHttpClientManager(
                context = context,
                connectivityChecker = NetworkConnectivityChecker,
                connectTimeout = settingsRepository.getSettings().connectTimeout,
                retryOnConnectionFailure = settingsRepository.getSettings().retryOnConnectionFailure,
                apiKeyProvider = {
                    runBlocking {
                        cacheRepo.getNotificationReaderKey(BuildConfig.API_KEY_NOTIFICATION_READER)
                    }
                },
                urlProvider = { hostManager.get().baseUrl }
            ) {
                EntryPointAccessors.fromApplication(baseApplicationContext, ModuleAppEntryPoint::class.java)
                    .notificationReaderRetrofit().instance
            }.build()
        }
    }
}