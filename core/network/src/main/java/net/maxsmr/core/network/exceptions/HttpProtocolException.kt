package net.maxsmr.core.network.exceptions

import android.text.TextUtils
import net.maxsmr.core.network.asString
import net.maxsmr.core.network.asStringCopy
import net.maxsmr.core.network.headersToMap
import okhttp3.Response

/**
 * Базовая ошибка при получении ответа не 2xx с разобранными полями ответа;
 * может содержать в себе исходный [HttpException];
 */
open class HttpProtocolException(
    val url: String = "",
    val method: String = "",
    val headers: Map<String, String> = mapOf(),
    val httpCode: Int = HTTP_ERROR_CODE_UNKNOWN,
    val httpMessage: String = "",
    val requestBodyString: String = "",
    val errorBodyString: String = "",
    message: String = httpMessage
) : NetworkException(httpCode, message) {

    protected constructor(builder: Builder) : this(
            builder.url,
            builder.method,
            builder.headers,
            builder.httpCode,
            builder.httpMessage,
            builder.requestBodyString,
            builder.errorBodyString,
            prepareMessage(
                    builder.url,
                    builder.method,
                    builder.httpMessage,
                    builder.httpCode.toString(),
                    builder.errorBodyString
            )
    )

    /**
     * конструктор копирования из [HttpProtocolException]
     */
    constructor(source: HttpProtocolException) : this(
            source.url,
            source.method,
            source.headers,
            source.httpCode,
            source.httpMessage,
            source.requestBodyString,
            source.errorBodyString
    )

    override fun toString(): String {
        return "HttpProtocolException(url='$url'," +
                "method='$method'," +
                "headers=$headers," +
                "httpCode=$httpCode," +
                "httpMessage='$httpMessage'," +
                "requestBodyString='$requestBodyString'," +
                "errorBodyString='$errorBodyString')"
    }

    open class Builder(
            rawResponse: Response?
    ) {

        val httpCode: Int = rawResponse?.code ?: HTTP_ERROR_CODE_UNKNOWN
        val httpMessage: String = rawResponse?.message.orEmpty()

        val url: String
        val method: String
        val headers: Map<String, String>

        val requestBodyString: String
        val errorBodyString: String

        init {
            val request = rawResponse?.request
            url = request?.url?.toString().orEmpty()
            method = request?.method.orEmpty()
            headers = request?.headers.headersToMap()
            requestBodyString = request.asString().orEmpty()
            errorBodyString = rawResponse.asStringCopy()?.first.orEmpty()
        }

        open fun build() = HttpProtocolException(this)
    }

    companion object {

        private const val HTTP_ERROR_CODE_UNKNOWN = -1

        fun prepareMessage(vararg parts: String): String {
            val partsList = mutableListOf<String>()
            partsList.addAll(parts)
            return TextUtils.join(", ", partsList.filter {
                it.isNotEmpty()
            })
        }
    }
}
