package net.maxsmr.core.network.retrofit.client.okhttp

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

abstract class BaseOkHttpClientManager(
    private val callTimeout: Long,
    private val readTimeout: Long = 0L,
    private val writeTimeout: Long = 0L,
    private val connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
) {

    protected abstract fun OkHttpClient.Builder.configureBuild()

    fun build(): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()
            .callTimeout(callTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
        clientBuilder.configureBuild()
        return clientBuilder.build()
    }

    companion object {

        const val CONNECT_TIMEOUT_DEFAULT = 10L
    }
}