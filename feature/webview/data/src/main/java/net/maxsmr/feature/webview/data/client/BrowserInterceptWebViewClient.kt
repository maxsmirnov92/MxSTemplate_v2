package net.maxsmr.feature.webview.data.client

import android.content.Context
import net.maxsmr.core.ui.openSystemBrowserWithToastError
import okhttp3.OkHttpClient
import net.maxsmr.feature.webview.data.client.interceptor.IWebViewInterceptor
import net.maxsmr.feature.webview.data.client.interceptor.IWebViewInterceptor.InterceptedUrl
import net.maxsmr.feature.webview.data.client.interceptor.WebViewInterceptor

/**
 * [InterceptWebViewClient] с переходом во внешний браузер, когда возникает редирект по урлам
 */
open class BrowserInterceptWebViewClient @JvmOverloads constructor(
    context: Context,
    okHttpClient: OkHttpClient? = null,
    webViewInterceptor: IWebViewInterceptor = WebViewInterceptor()
): InterceptWebViewClient(context, okHttpClient, webViewInterceptor) {

    final override fun shouldInterceptFromOverrideUrl(url: String): InterceptedUrl? {
        return if (shouldOpenSystemBrowser(url)) {
            context.openSystemBrowserWithToastError(url)
            InterceptedUrl(InterceptedUrl.Type.OK, url)
        } else {
            null
        }
    }

    protected open fun shouldOpenSystemBrowser(url: String): Boolean = true
}