package net.maxsmr.notification_reader.di.holder

import android.content.Context
import kotlinx.serialization.json.Json
import net.maxsmr.core.BaseKeyInstanceHolder
import net.maxsmr.core.network.client.okhttp.interceptors.UrlChangeInterceptor
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.notification_reader.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File

/**
 * Для использования [CommonRetrofitClient] при динамической базовой урле.
 * Не рекомендуется, т.к. есть [UrlChangeInterceptor]
 */
class NotificationReaderRetrofitHolder(
    private val hostManager: NotificationReaderHostManagerHolder,
    okHttpClient: NotificationReaderOkHttpClientHolder,
    context: Context,
    json: Json,
) : BaseKeyInstanceHolder<String, CommonRetrofitClient>({
    CommonRetrofitClient(
        hostManager.get().baseUrl.toHttpUrl(),
        json,
        File(context.cacheDir, "OkHttpCache").path,
        BuildConfig.PROTOCOL_VERSION,
        true
    ) {
        okHttpClient.get()
    }.apply {
        init()
    }
}) {

    override val key: String
        get() = hostManager.key
}