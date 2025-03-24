package net.maxsmr.core.network.client.okhttp

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class PicassoOkHttpClientManager(
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    private val forceCacheInterceptor: Interceptor,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
) : BaseOkHttpClientManager(connectTimeout) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(httpLoggingInterceptor)
            addInterceptor(forceCacheInterceptor)
        }
    }
}