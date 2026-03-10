package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.okhttp.appendValues
import net.maxsmr.core.network.client.okhttp.interceptors.annotations.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.annotations.ServiceFields
import net.maxsmr.core.network.okhttp.hasAnnotation
import net.maxsmr.core.network.session.SessionStorage
import okhttp3.Interceptor
import okhttp3.Response

class AdditionalInfoInterceptor(
    private val deviceGuid: String,
    private val platform: String,
    private val version: String,
    private val sessionStorage: SessionStorage?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
            val hasAuthorization = request.hasAnnotation<Authorization>()
            val hasServiceFields = request.hasAnnotation<ServiceFields>()
            val needSession = sessionStorage != null && hasAuthorization
            val needServiceInfo = hasServiceFields
            if (needServiceInfo || needSession) {
                request = request.appendValues {
                    if (needServiceInfo) {
                        put("deviceGuid", deviceGuid)
                        put("platform", platform)
                        put("version", version)
                    }
                    if (needSession) {
                        putOpt("session", sessionStorage.session)
                    }
                }
            }
        return chain.proceed(request)
    }
}