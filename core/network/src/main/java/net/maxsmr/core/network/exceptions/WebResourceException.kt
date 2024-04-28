package net.maxsmr.core.network.exceptions

import android.webkit.WebViewClient

class WebResourceException(
    code: Int,
    message: String = "",
) : NetworkException(code, message) {

    val isConnectionError =
        code in listOf(WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_IO, WebViewClient.ERROR_TIMEOUT)

    override fun toString(): String {
        return "WebResourceException(code=$code, message=$message, isConnectionError=$isConnectionError)"
    }

    companion object {

        const val ERROR_RESPONSE_EMPTY = -17

        fun emptyWebResourceException() = WebResourceException(ERROR_RESPONSE_EMPTY, "Response is empty")
    }
}