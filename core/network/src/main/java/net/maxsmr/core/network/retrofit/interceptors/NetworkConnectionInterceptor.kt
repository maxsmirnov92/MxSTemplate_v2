package net.maxsmr.core.network.retrofit.interceptors

import net.maxsmr.core.network.exceptions.NoConnectivityException
import okhttp3.Interceptor
import okhttp3.Response

class NetworkConnectionInterceptor(
    private val connectivityChecker: ConnectivityChecker,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!connectivityChecker.isConnected()) {
            throw NoConnectivityException()
        }

        return chain.proceed(chain.request())
    }
}

interface ConnectivityChecker {

    fun isConnected(): Boolean
}