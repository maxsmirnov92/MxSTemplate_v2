package net.maxsmr.core.network.client.okhttp

import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class DownloadOkHttpClientManager(
    private val connectivityChecker: ConnectivityChecker,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    timeout: Long,
    retryOnConnectionFailure: Boolean = true
): BaseOkHttpClientManager(0, timeout, timeout, timeout, retryOnConnectionFailure) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            addInterceptor(NetworkConnectionInterceptor(connectivityChecker))
            addInterceptor(httpLoggingInterceptor)
        }
    }
}