package net.maxsmr.core.network.client.okhttp

import android.content.Context
import androidx.annotation.CallSuper
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.ResponseErrorMessageInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * @param [retrofitProvider] при наличии [Retrofit] будет задейстоваться [ResponseErrorMessageInterceptor]
 */
abstract class BaseRestOkHttpClientManager(
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    readTimeout: Long = connectTimeout,
    writeTimeout: Long = connectTimeout,
    callTimeout: Long = 0L,
    retryOnConnectionFailure: Boolean = RETRY_ON_CONNECTION_FAILURE_DEFAULT,
    private val context: Context,
    private val connectivityChecker: ConnectivityChecker,
    private val responseAnnotation: Annotation? = null,
    private val retrofitProvider: (() -> Retrofit)? = null,
) : BaseOkHttpClientManager(connectTimeout, readTimeout, writeTimeout, callTimeout, retryOnConnectionFailure) {

    @CallSuper
    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            val loggingInterceptor = ApiLoggingInterceptor { message: String ->
                logger.d(message)
            }.apply {
                setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
            }
            addInterceptor(loggingInterceptor)
            addInterceptor(NetworkConnectionInterceptor(context, connectivityChecker))
            retrofitProvider?.let {
                addInterceptor(ResponseErrorMessageInterceptor(responseAnnotation, it))
            }
        }
    }
}