package net.maxsmr.feature.webview.data.client.exception

import android.webkit.WebViewClient
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient

class WebResourceException(
    val webViewData: InterceptWebViewClient.WebViewData? = null,
    code: Int,
    message: String = EMPTY_STRING,
) : NetworkException(code, message) {

    val isConnectionError =
        code in listOf(WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_IO, WebViewClient.ERROR_TIMEOUT)

    override fun toString(): String {
        return "WebResourceException(code=$code, message=$message, isConnectionError=$isConnectionError)"
    }

    companion object {

        const val ERROR_RESPONSE_EMPTY = -17

        fun emptyWebResourceException(url: String? = null) = WebResourceException(
            if (!url.isNullOrEmpty()) {
                InterceptWebViewClient.WebViewData(url, true, null, null)
            } else {
                null
            },
            ERROR_RESPONSE_EMPTY,
            "Response is empty"
        )


    }
}