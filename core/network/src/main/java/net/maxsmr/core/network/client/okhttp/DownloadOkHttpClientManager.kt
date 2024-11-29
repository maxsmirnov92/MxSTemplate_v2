package net.maxsmr.core.network.client.okhttp

import android.content.Context
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class DownloadOkHttpClientManager(
    private val context: Context,
    private val connectivityChecker: ConnectivityChecker,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    retryOnConnectionFailure: Boolean = true,
) : BaseOkHttpClientManager(connectTimeout, retryOnConnectionFailure = retryOnConnectionFailure) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(NetworkConnectionInterceptor(context, connectivityChecker))
            addInterceptor(httpLoggingInterceptor)
        }
    }
}