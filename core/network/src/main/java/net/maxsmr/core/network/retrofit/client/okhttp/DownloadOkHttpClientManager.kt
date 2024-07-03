package net.maxsmr.core.network.retrofit.client.okhttp

import net.maxsmr.core.network.retrofit.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class DownloadOkHttpClientManager(
    private val networkConnectionInterceptor: NetworkConnectionInterceptor,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    timeout: Long,
    retryOnConnectionFailure: Boolean = true
): BaseOkHttpClientManager(0, timeout, timeout, timeout, retryOnConnectionFailure) {

    override fun OkHttpClient.Builder.configureBuild() {
        addInterceptor(networkConnectionInterceptor)
        addInterceptor(httpLoggingInterceptor)
    }
}