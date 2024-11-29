package net.maxsmr.core.network.client.okhttp

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class PicassoOkHttpClientManager(
    private val forceCacheInterceptor: Interceptor,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
) : BaseOkHttpClientManager(connectTimeout) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(forceCacheInterceptor)
            addInterceptor(httpLoggingInterceptor)
        }
    }
}