package net.maxsmr.core.network.client.okhttp

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class PicassoOkHttpClientManager(
    private val forceCacheInterceptor: Interceptor,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    timeout: Long,
): BaseOkHttpClientManager(0, timeout, timeout, timeout) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            addInterceptor(forceCacheInterceptor)
            addInterceptor(httpLoggingInterceptor)
        }
    }
}