package net.maxsmr.core.network.retrofit.client.okhttp

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class PicassoOkHttpClientManager(
    private val forceCacheInterceptor: Interceptor,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    timeout: Long,
): BaseOkHttpClientManager(timeout, timeout, timeout) {

    override fun OkHttpClient.Builder.configureBuild() {
        addInterceptor(forceCacheInterceptor)
        addInterceptor(httpLoggingInterceptor)
    }
}