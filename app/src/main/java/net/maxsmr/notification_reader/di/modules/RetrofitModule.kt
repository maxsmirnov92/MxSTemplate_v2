package net.maxsmr.notification_reader.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.NotificationReaderHostManager
import net.maxsmr.core.di.NotificationReaderOkHttpClient
import net.maxsmr.core.di.NotificationReaderRetrofit
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.notification_reader.BuildConfig
import net.maxsmr.notification_reader.di.holder.NotificationReaderHostManagerHolder
import net.maxsmr.notification_reader.di.holder.NotificationReaderOkHttpClientHolder
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class RetrofitModule {

    @[Provides Singleton NotificationReaderRetrofit]
    fun provideNotificationReaderRetrofit(
        @ApplicationContext context: Context,
        @NotificationReaderHostManager hostManager: NotificationReaderHostManagerHolder,
        @NotificationReaderOkHttpClient okHttpClient: NotificationReaderOkHttpClientHolder,
        @BaseJson json: Json,
    ): CommonRetrofitClient = CommonRetrofitClient(
        // Меняется в самом OkHttpClient на каждый intercept,
        // но в самом Retrofit зафиксируется только при инициализации
        hostManager.get().baseUrl.toHttpUrl(),
        json,
        File(context.cacheDir, "OkHttpCache").path,
        BuildConfig.PROTOCOL_VERSION,
        false
        // cacheManager.getDisableCache()
    ) {
        okHttpClient.get()
    }
}