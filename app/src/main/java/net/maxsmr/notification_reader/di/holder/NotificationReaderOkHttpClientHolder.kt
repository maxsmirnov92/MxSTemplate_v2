package net.maxsmr.notification_reader.di.holder

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.BaseKeyInstanceHolder
import net.maxsmr.core.android.network.NetworkConnectivityChecker
import net.maxsmr.core.network.client.okhttp.NotificationReaderOkHttpClientManager
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.BuildConfig
import net.maxsmr.notification_reader.di.ModuleAppEntryPoint
import okhttp3.OkHttpClient

class NotificationReaderOkHttpClientHolder(
    private val settingsRepository: SettingsDataStoreRepository,
    hostManager: NotificationReaderHostManagerHolder,
    context: Context,
) : BaseKeyInstanceHolder<NotificationReaderOkHttpClientHolder.Key, OkHttpClient>({
    NotificationReaderOkHttpClientManager(
        context = context,
        connectivityChecker = NetworkConnectivityChecker,
        connectTimeout = it.timeout,
        retryOnConnectionFailure = it.retryOnConnectionFailure,
        apiKeyProvider = {
            // не пересоздавать OkHttpClient при смене ключа - подставлять в Interceptor
            runBlocking {
                settingsRepository.getSettings().notificationsApiKey
            }
        },
        urlProvider = { hostManager.get().uri.toString() }
    ) {
        EntryPointAccessors.fromApplication(context, ModuleAppEntryPoint::class.java)
            .notificationReaderRetrofit().instance
    }.build()
}) {

    override val key: Key
        get() = runBlocking {
            Key(
                settingsRepository.getSettings().connectTimeout,
                settingsRepository.getSettings().retryOnConnectionFailure
            )
        }

    data class Key(
        val timeout: Long,
        val retryOnConnectionFailure: Boolean,
    )
}