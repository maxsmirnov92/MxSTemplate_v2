package net.maxsmr.feature.webview.data.client

import android.webkit.WebChromeClient
import android.webkit.WebView

open class BaseWebChromeClient : WebChromeClient() {

    var onProgressChanged: ((Int) -> Unit)? = null

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged?.invoke(newProgress)
    }
}