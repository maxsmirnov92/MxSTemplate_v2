package net.maxsmr.core.network.retrofit.client.okhttp

import net.maxsmr.core.network.retrofit.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class DownloadOkHttpClientManager(
    private val networkConnectionInterceptor: NetworkConnectionInterceptor,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    timeout: Long,
): BaseOkHttpClientManager(0, timeout, timeout, timeout) {

    override fun OkHttpClient.Builder.configureBuild() {
        addInterceptor(networkConnectionInterceptor)
        addInterceptor(httpLoggingInterceptor)
    }
}