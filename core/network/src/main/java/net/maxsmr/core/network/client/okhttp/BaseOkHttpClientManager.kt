package net.maxsmr.core.network.client.okhttp

import androidx.annotation.CallSuper
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.client.okhttp.interceptors.ResponseErrorMessageInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * @param [retrofitProvider] при наличии [Retrofit] будет задейстоваться [ResponseErrorMessageInterceptor]
 */
abstract class BaseOkHttpClientManager(
    private val callTimeout: Long,
    private val readTimeout: Long = 0L,
    private val writeTimeout: Long = 0L,
    private val connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    private val retryOnConnectionFailure: Boolean = RETRY_ON_CONNECTION_FAILURE_DEFAULT,
) {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    protected abstract fun configureBuild(builder: OkHttpClient.Builder)

    fun build(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            withTimeouts(
                connectTimeout, readTimeout, writeTimeout, callTimeout
            )
            retryOnConnectionFailure(retryOnConnectionFailure)
            configureBuild(this)
        }.build()
    }

    companion object {

        const val CONNECT_TIMEOUT_DEFAULT = 10L
        const val RETRY_ON_CONNECTION_FAILURE_DEFAULT = true

        fun OkHttpClient.Builder.withTimeouts(
            connectTimeout: Long,
            readTimeout: Long = connectTimeout,
            writeTimeout: Long = connectTimeout,
            callTimeout: Long = 0,
        ) {
            this.callTimeout(callTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        }
    }
}