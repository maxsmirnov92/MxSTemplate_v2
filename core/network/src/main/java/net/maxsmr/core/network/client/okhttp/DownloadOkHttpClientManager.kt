package net.maxsmr.core.network.client.okhttp

import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class DownloadOkHttpClientManager(
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    private val connectionInterceptor: NetworkConnectionInterceptor,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    retryOnConnectionFailure: Boolean = true,
) : BaseOkHttpClientManager(connectTimeout, retryOnConnectionFailure = retryOnConnectionFailure) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(httpLoggingInterceptor)
            addInterceptor(connectionInterceptor)
        }
    }
}