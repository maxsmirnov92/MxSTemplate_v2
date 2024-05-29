package net.maxsmr.feature.webview.data.client.exception

import android.webkit.WebViewClient
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.exceptions.NetworkException

open class WebResourceException(
    code: Int,
    message: String = EMPTY_STRING,
) : NetworkException(code, message) {

    val isConnectionError =
        code in listOf(WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_IO, WebViewClient.ERROR_TIMEOUT)

    override fun toString(): String {
        return "WebResourceException(code=$code, message=$message, isConnectionError=$isConnectionError)"
    }
}