package net.maxsmr.feature.webview.data.client.exception

class EmptyWebResourceException: WebResourceException(ERROR_RESPONSE_EMPTY, "Response is empty") {

    companion object {

        private const val ERROR_RESPONSE_EMPTY = -17
    }
}