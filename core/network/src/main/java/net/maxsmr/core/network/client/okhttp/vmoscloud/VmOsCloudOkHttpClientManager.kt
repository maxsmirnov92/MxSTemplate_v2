package net.maxsmr.core.network.client.okhttp.vmoscloud

import android.content.Context
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.network.HEADER_CONTENT_TYPE
import net.maxsmr.core.network.appendValues
import net.maxsmr.core.network.asStringOrThrow
import net.maxsmr.core.network.client.okhttp.BaseRestOkHttpClientManager
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.toQueryMap
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Invocation
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class VmOsCloudOkHttpClientManager(
    private val host: String,
    private val accessKeyId: String,
    private val secretAccessKey: String,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    context: Context,
    connectivityChecker: ConnectivityChecker,
    cache: ResponseBodyCache<*>,
) : BaseRestOkHttpClientManager(
    connectTimeout,
    context = context,
    connectivityChecker = connectivityChecker,
    cache = cache
) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(VmOsCloudInterceptor())
        }
    }

    internal inner class VmOsCloudInterceptor : Interceptor {

        private val serviceName = "armcloud-paas"
        private val contentType = "application/json;charset=UTF-8"

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            if (invocation != null) {
                request = request.appendValues(appendHeadersFunc = {

                    val xDate = LocalDateTime.now().format()

                    addHeader("x-host", host)
                    addHeader("x-date", xDate)

                    val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                    if (needAuthorization) {
                        val authorizationHeader = getAuthorizationHeader(
                            xDate,
                            if (request.method == Method.GET.value) {
                                request.queryParamsToJson().toString()
                            } else {
                                request.asStringOrThrow()
                            }
                        )
                        addHeader("authorization", authorizationHeader)
                    }

                    addHeader(HEADER_CONTENT_TYPE, contentType)
                })
            }

            return chain.proceed(request)
        }

        private fun getSign(
            xDate: String,
            requestBody: String,
        ): String {
            return PaasSignUtils.signature(
                contentType,
                "content-type;host;x-content-sha256;x-date",
                host,
                xDate,
                secretAccessKey,
                serviceName,
                requestBody.toByteArray(StandardCharsets.UTF_8),
            )
        }

        private fun getAuthorizationHeader(currentTimestamp: String, body: String): String {
            try {
                val sign = getSign(currentTimestamp, body)
                return String.format(
                    "HMAC-SHA256 Credential=%s, SignedHeaders=content-type;host;x-content-sha256;x-date, Signature=%s",
                    accessKeyId,
                    sign
                )
            } catch (e: java.lang.Exception) {
                throw RuntimeException("Failed to generate signature", e)
            }
        }

        private fun Request.queryParamsToJson(): JSONObject {
            val json = JSONObject()
            url.toQueryMap().forEach {
                json.put(it.key, it.value)
            }
            return json
        }

        private fun LocalDateTime.format(): String {
            val formatter = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'")
            return format(formatter)
        }
    }
}