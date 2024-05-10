package net.maxsmr.core.network.exceptions

import android.text.TextUtils
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.UNKNOWN_ERROR
import net.maxsmr.core.network.asString
import net.maxsmr.core.network.asStringCopy
import net.maxsmr.core.network.toMap
import okhttp3.Response

/**
 * Базовая ошибка при получении ответа не 2xx с разобранными полями ответа
 */
open class HttpProtocolException(
    val url: String,
    val method: String,
    val requestHeaders: HashMap<String, String> = hashMapOf(),
    val requestBodyString: String = EMPTY_STRING,
    val responseCode: Int = UNKNOWN_ERROR,
    val responseMessage: String = EMPTY_STRING,
    val responseBodyString: String = EMPTY_STRING,
    val responseBodyHeaders: HashMap<String, String> = hashMapOf(),
    message: String?,
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
                "requestHeaders=$requestHeaders, " +
                "requestBodyString='$requestBodyString', " +
                "responseCode=$responseCode, " +
                "responseMessage='$responseMessage', " +
//                "responseBodyString='$responseBodyString', " +
                "responseBodyHeaders=$responseBodyHeaders)"
    }

    companion object {

        @JvmStatic
        fun Response?.toHttpProtocolException(exceptionMessage: String? = null): HttpProtocolException {
            val request = this?.request
            val url = request?.url?.toString().orEmpty()
            val method = request?.method.orEmpty()
            val code = this?.code ?: UNKNOWN_ERROR
            val message = this?.message.orEmpty()
            val responseBody = this.asStringCopy()?.first.orEmpty()
            return HttpProtocolException(
                url,
                method,
                HashMap(request?.headers.toMap()),
                request.asString().orEmpty(),
                code,
                message,
                responseBody,
                HashMap(this?.headers.toMap()),
                exceptionMessage?.takeIf { it.isNotEmpty() }
                    ?: prepareMessage(
                        url,
                        method,
                        code.toString(),
                        message,
                        responseBody
                    ))
        }

        private fun prepareMessage(vararg parts: String): String {
            val partsList = mutableListOf<String>()
            partsList.addAll(parts)
            return TextUtils.join(", ", partsList.filter {
                it.isNotEmpty()
            })
        }
    }
}
