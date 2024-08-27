package net.maxsmr.core.network.client.okhttp

import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class DownloadOkHttpClientManager(
    private val networkConnectionInterceptor: NetworkConnectionInterceptor,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    timeout: Long,
    retryOnConnectionFailure: Boolean = true
): BaseOkHttpClientManager(0, timeout, timeout, timeout, retryOnConnectionFailure) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(networkConnectionInterceptor)
            addInterceptor(httpLoggingInterceptor)
        }
    }
}