package net.maxsmr.core.network.exceptions

import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.UNKNOWN_ERROR
import net.maxsmr.core.network.asString
import net.maxsmr.core.network.asStringCloned
import net.maxsmr.core.network.toPairs
import okhttp3.Response

/**
 * Базовая ошибка при получении ответа не 2xx с разобранными полями ответа;
 * или при несоответствии Response иным критериям
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
    message: String = EMPTY_STRING,
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
        source.message.orEmpty()
    )

    constructor(
        response: Response?,
        exceptionMessage: String? = null,
        withBody: Boolean = false,
    ) : this(
        response?.request?.url?.toString().orEmpty(),
        response?.request?.method.orEmpty(),
        ArrayList(response?.request?.headers.toPairs()),
        if (withBody) response?.request.asString().orEmpty() else EMPTY_STRING,
        response?.code ?: UNKNOWN_ERROR,
        response?.message.orEmpty(),
            if (withBody) response.asStringCloned()?.first.orEmpty() else EMPTY_STRING,
        ArrayList(response?.headers.toPairs()),
        exceptionMessage?.takeIf { it.isNotEmpty() } ?: response?.defaultMessage().orEmpty()
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

        private fun Response.defaultMessage(): String {
            return buildMessage(
                request.url.toString(),
                request.method,
                code.toString(),
                message
            )
        }

        private fun buildMessage(vararg parts: String): String {
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
