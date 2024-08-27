package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.asString
import net.maxsmr.core.network.client.okhttp.interceptors.InterceptorUtils.toJSONObject
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Invocation

class AdditionalInfoInterceptor(
    private val deviceGuid: String,
    private val platform: String,
    private val version: String,
//    private val sessionStorage: SessionStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        if (invocation != null) {
            request.body?.let { requestBody ->
                val serviceFields = invocation.method().getAnnotation(ServiceFields::class.java)
                val needServiceInfo = serviceFields != null
                val needSession = invocation.method().getAnnotation(Session::class.java) != null

                if (needServiceInfo || needSession) {
                    val subtype = requestBody.contentType()?.subtype
                    if (subtype == null || subtype.contains("json", true)) {
                        if (subtype == null) {
                            request = request.addHeaderFields()
                        }

                        val json = request.asString()?.toJSONObject()

//                        val sessionId: String = if (needSession) {
//                            sessionStorage.session.orEmpty()
//                        } else {
//                            EMPTY_STRING
//                        }

                        request = request.newBuilder().tag(JSONObject::class.java, json)
                            .post(json.toString().toRequestBody(requestBody.contentType())).build()
                    }
                }
            }
        }

        return chain.proceed(request)
    }

    private fun JSONObject.addServiceInfo() {
        put("deviceGuid", deviceGuid)
        put("platform", platform)
        put("version", version)
    }

    private fun Request.addHeaderFields() = newBuilder()
        .addHeader("Content-Type", "application/json")
        .build()
}