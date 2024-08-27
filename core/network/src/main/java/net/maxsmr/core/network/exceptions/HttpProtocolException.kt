package net.maxsmr.core.network.exceptions

import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.UNKNOWN_ERROR
import net.maxsmr.core.network.asString
import net.maxsmr.core.network.asStringCloned
import net.maxsmr.core.network.toPairs
import okhttp3.Response

/**
 * Базовая ошибка при получении ответа не 2xx с разобранными полями ответа
 */
open class HttpProtocolException(
    val url: String,
    val method: String,
    val requestHeaders: ArrayList<Pair<String, String>> = arrayListOf(),
    val requestBodyString: String = EMPTY_STRING,
    val responseCode: Int = UNKNOWN_ERROR,
    val responseMessage: String = EMPTY_STRING,
    val responseBodyString: String = EMPTY_STRING,
    val responseBodyHeaders: ArrayList<Pair<String, String>> = arrayListOf(),
    message: String? = null,
) : NetworkException(responseCode, message) {

    constructor(source: HttpProtocolException) : this(
        source.url,
        source.method,
        source.requestHeaders,
        source.requestBodyString,
        source.responseCode,
        source.responseMessage,
        source.responseBodyString,
        source.responseBodyHeaders,
        source.message
    )

    override fun toString(): String {
        return "HttpProtocolException(url='$url', " +
                "method='$method', " +
                "requestHeaders=${requestHeaders.headersToString()}, " +
                "requestBodyString='$requestBodyString', " +
                "responseCode=$responseCode, " +
                "responseMessage='$responseMessage', " +
                "responseBodyString='$responseBodyString', " +
                "responseBodyHeaders=${responseBodyHeaders.headersToString()}, " +
                "message=$message)"
    }

    companion object {

        @JvmStatic
        fun Response?.toHttpProtocolException(
            exceptionMessage: String? = null,
            withBody: Boolean = false
        ): HttpProtocolException {
            val request = this?.request
            val url = request?.url?.toString().orEmpty()
            val method = request?.method.orEmpty()
            val code = this?.code ?: UNKNOWN_ERROR
            val message = this?.message.orEmpty()
            val responseBody =  if (withBody) this.asStringCloned()?.first.orEmpty() else EMPTY_STRING
            return HttpProtocolException(
                url,
                method,
                ArrayList(request?.headers.toPairs()),
                if (withBody) request.asString().orEmpty() else EMPTY_STRING,
                code,
                message,
                responseBody,
                ArrayList(this?.headers.toPairs()),
                exceptionMessage?.takeIf { it.isNotEmpty() }
                    ?: prepareMessage(
                        url,
                        method,
                        code.toString(),
                        message
                    ))
        }

        private fun prepareMessage(vararg parts: String): String {
            val partsList = mutableListOf<String>()
            partsList.addAll(parts)
            return partsList.filter {
                it.isNotEmpty()
            }.joinToString(", ")
        }

        private fun ArrayList<Pair<String, String>>.headersToString(): String {
            return joinToString(", ") { "${it.first}=${it.second}" }
        }
    }
}
