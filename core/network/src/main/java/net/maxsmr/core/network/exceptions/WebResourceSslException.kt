package net.maxsmr.core.network.exceptions

import android.net.http.SslError
class WebResourceSslException(val error: SslError): NetworkException(error.primaryError, error.toString()) {

    override fun toString(): String {
        return "WebResourceSslException(error=$error)"
    }
}