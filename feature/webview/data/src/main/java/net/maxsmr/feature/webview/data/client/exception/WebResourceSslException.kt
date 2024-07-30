package net.maxsmr.feature.webview.data.client.exception

import android.net.http.SslError
import net.maxsmr.core.network.exceptions.NetworkException

class WebResourceSslException(val error: SslError): WebResourceException(error.primaryError, error.toString()) {

    override fun toString(): String {
        return "WebResourceSslException(error=$error)"
    }
}