package net.maxsmr.feature.webview.ui

import android.net.Uri
import android.text.TextUtils
import androidx.annotation.Keep
import net.maxsmr.commonutils.CHARSET_DEFAULT
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.android.network.equalsIgnoreSubDomain
import java.io.Serializable

/**
 * Инкапсулирет задание свойств для [android.webkit.WebView]
 */
@Keep
class WebViewCustomizer private constructor(
    val title: String,
    val url: String,
    val data: WebViewDataArgs?,
    val reloadAfterConnectionError: Boolean,
    val changeTitleOnLoad: Boolean,
    val strategy: ExternalViewUrlStrategy
): Serializable {

    fun buildUpon() = Builder()
        .setTitle(title)
        .setUrl(url)
        .setData(data)
        .setReloadAfterConnectionError(reloadAfterConnectionError)
        .setChangeTitleOnLoad(changeTitleOnLoad)
        .setBrowserInterceptStrategy(strategy)

    class Builder {

        private var title: String = EMPTY_STRING
        private var url: String  = EMPTY_STRING
        private var data: WebViewDataArgs? = null
        private var reloadAfterConnectionError: Boolean = true
        private var changeTitleOnLoad: Boolean = true
        private var viewUrlStrategy: ExternalViewUrlStrategy = ExternalViewUrlStrategy.NonBrowserFirst

        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setUrl(url: String?): Builder {
            this.url = if (TextUtils.isEmpty(url)) {
                EMPTY_STRING
            } else {
                val uri = Uri.parse(url)
                if (uri.scheme.isNullOrEmpty()) {
                    uri.buildUpon().scheme("http").build()
                } else {
                    uri
                }.toString()
            }
            return this
        }

        fun setData(data: WebViewDataArgs?): Builder {
            this.data = data
            return this
        }

        fun setReloadAfterConnectionError(toggle: Boolean): Builder {
            this.reloadAfterConnectionError = toggle
            return this
        }

        fun setChangeTitleOnLoad(toggle: Boolean): Builder {
            this.changeTitleOnLoad = true
            return this
        }

        fun setBrowserInterceptStrategy(strategy: ExternalViewUrlStrategy): Builder {
            this.viewUrlStrategy = strategy
            return this
        }

        fun build() = WebViewCustomizer(
            title,
            url,
            data,
            reloadAfterConnectionError,
            changeTitleOnLoad,
            viewUrlStrategy
        )
    }

    data class WebViewDataArgs @JvmOverloads constructor(
        val data: String,
        val mimeType: String = FileFormat.HTML.mimeType,
        val charset: String = CHARSET_DEFAULT,
        val forceBase64: Boolean = true
    ): Serializable

    sealed class ExternalViewUrlStrategy: Serializable {

        data object None : ExternalViewUrlStrategy() {

            private fun readResolve(): Any = None
        }

        data class UrlMatch(
            val protocols: List<String>,
            val hosts: List<String>,
            val queryParameters: List<String>,
        ): ExternalViewUrlStrategy() {

            fun match(uri: Uri): Boolean {
                val protocolPassed = if (protocols.isNotEmpty()) {
                    protocols.any {
                        uri.scheme?.equals(it) == true
                    }
                } else {
                    true
                }
                val hostPassed = if (hosts.isNotEmpty()) {
                    hosts.any {
                        uri.host.equalsIgnoreSubDomain(it)
                    }
                } else {
                    true
                }
                val parametersPassed = if (queryParameters.isNotEmpty()) {
                    uri.queryParameterNames.any { name ->
                        queryParameters.any { name == it }
                    }
                } else {
                    true
                }
                return protocolPassed && hostPassed && parametersPassed
            }
        }

        data object NonBrowserFirst : ExternalViewUrlStrategy() {

            private fun readResolve(): Any = NonBrowserFirst
        }
    }
}