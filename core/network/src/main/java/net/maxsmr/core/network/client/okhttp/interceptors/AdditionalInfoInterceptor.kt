package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.SessionStorage
import net.maxsmr.core.network.appendValues
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

class AdditionalInfoInterceptor(
    private val deviceGuid: String,
    private val platform: String,
    private val version: String,
    private val sessionStorage: SessionStorage?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        if (invocation != null) {
            val authorization = invocation.method().getAnnotation(Authorization::class.java)
            val serviceFields = invocation.method().getAnnotation(ServiceFields::class.java)
            val needSession = sessionStorage != null && authorization != null
            val needServiceInfo = serviceFields != null
            if (needServiceInfo || needSession) {
                request = request.appendValues {
                    if (needServiceInfo) {
                        put("deviceGuid", deviceGuid)
                        put("platform", platform)
                        put("version", version)
                    }
                    if (needSession) {
                        putOpt("session", sessionStorage?.session)
                    }
                }
            }
        }
        return chain.proceed(request)
    }
}