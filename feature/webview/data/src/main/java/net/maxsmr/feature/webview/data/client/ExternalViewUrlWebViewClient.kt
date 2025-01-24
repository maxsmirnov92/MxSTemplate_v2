package net.maxsmr.feature.webview.data.client

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import net.maxsmr.commonutils.isAtLeastS
import net.maxsmr.commonutils.openViewUrl
import net.maxsmr.commonutils.openViewUrlNonBrowser
import net.maxsmr.core.network.URL_SCHEME_HTTP
import net.maxsmr.core.network.URL_SCHEME_HTTPS
import net.maxsmr.core.ui.openViewUrlWithToastError
import net.maxsmr.feature.webview.data.client.interceptor.IWebViewInterceptor
import net.maxsmr.feature.webview.data.client.interceptor.IWebViewInterceptor.InterceptedUrl
import net.maxsmr.feature.webview.data.client.interceptor.WebViewInterceptor
import okhttp3.OkHttpClient

/**
 * [InterceptWebViewClient] с переходом во внешние приложения для [Intent.ACTION_VIEW] по текущей перехваченной урле
 */
open class ExternalViewUrlWebViewClient @JvmOverloads constructor(
    context: Context,
    okHttpClient: OkHttpClient? = null,
    webViewInterceptor: IWebViewInterceptor = WebViewInterceptor(),
    private val viewUrlModeForUrl: (String) -> ViewUrlMode = { ViewUrlMode.NON_BROWSER_EXTERNAL }
) : InterceptWebViewClient(context, okHttpClient, webViewInterceptor) {

    final override fun shouldInterceptFromOverrideUrl(url: String): InterceptedUrl? {
        val result = when (getViewUrlMode(url)) {
            ViewUrlMode.EXTERNAL -> {
                context.openViewUrl(url)
            }

            ViewUrlMode.EXTERNAL_WITH_UI -> {
                context.openViewUrlWithToastError(url)
            }

            ViewUrlMode.NON_BROWSER -> {
                openViewUrlNonBrowserCompat(url)
            }

            ViewUrlMode.NON_BROWSER_EXTERNAL -> {
                var handled = false
                if (isAtLeastS()) {
                    handled = openViewUrlNonBrowserCompat(url)
                }
                if (!handled) {
                    context.openViewUrl(url)
                } else {
                    false
                }
            }

            ViewUrlMode.NON_BROWSER_EXTERNAL_UI -> {
                var handled = false
                if (isAtLeastS()) {
                    handled = context.openViewUrlNonBrowser(url)
                }
                if (!handled) {
                    context.openViewUrlWithToastError(url)
                } else {
                    false
                }
            }

            ViewUrlMode.INTERNAL -> {
                false
            }
        }
        return if (result) {
            InterceptedUrl(InterceptedUrl.Type.OK, url)
        } else {
            null
        }
    }

    protected open fun getViewUrlMode(url: String): ViewUrlMode = viewUrlModeForUrl(url)

    private fun openViewUrlNonBrowserCompat(url: String): Boolean {
        return if (isAtLeastS()) {
            context.openViewUrlNonBrowser(url)
        } else {
            // если API ниже 31,
            // пробуем открыть с обычным ACTION_VIEW,
            // но только не для схем http/https
            val uri = url.toUri()
            if (uri.scheme !in listOf(URL_SCHEME_HTTP, URL_SCHEME_HTTPS)) {
                context.openViewUrl(url)
            } else {
                false
            }
        }
    }

    enum class ViewUrlMode {

        /*
         * Открытие в приложении(ях) для просмотра ссылок;
         * если не вышло - продолжает запрос в этом клиенте
         */
        EXTERNAL,

        /**
         * То же самое, что и предыдущее, только с тостом
         */
        EXTERNAL_WITH_UI,

        /**
         * Пытается сначала открыть аппом не браузером;
         * если не вышло - продолжает запрос в этом клиенте
         */
        NON_BROWSER,

        /**
         * Пытается сначала открыть аппом не браузером;
         * если не вышло - во внешнем браузере;
         * или продолжает запрос в этом клиенте
         */
        NON_BROWSER_EXTERNAL,

        /**
         * То же самое, что и предыдущее, только с тостом
         */
        NON_BROWSER_EXTERNAL_UI,

        /**
         * Обрабатывать урлу в этом клиенте
         */
        INTERNAL
    }
}