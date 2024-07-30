package net.maxsmr.feature.webview.data.client

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

open class ProgressWebChromeClient : WebChromeClient() {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    var onProgressChanged: ((Int) -> Unit)? = null

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged?.invoke(newProgress)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        logger.d("onConsoleMessage, level: ${consoleMessage.messageLevel()}, message: ${consoleMessage.message()}, line number: ${consoleMessage.lineNumber()}")
        return super.onConsoleMessage(consoleMessage)
    }
}