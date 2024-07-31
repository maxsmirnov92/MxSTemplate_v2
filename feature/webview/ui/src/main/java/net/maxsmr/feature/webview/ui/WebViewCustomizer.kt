package net.maxsmr.feature.webview.ui

import android.net.Uri
import androidx.annotation.Keep
import net.maxsmr.commonutils.CHARSET_DEFAULT
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.android.network.URL_PAGE_BLANK
import net.maxsmr.core.android.network.equalsIgnoreSubDomain
import net.maxsmr.core.android.network.isUrlValid
import java.io.Serializable

/**
 * Инкапсулирет задание свойств для [android.webkit.WebView]
 * @param url любая URL, включая [URL_PAGE_BLANK] или ресурсные схемы
 */
@Keep
class WebViewCustomizer private constructor(
    val title: String,
    val url: String,
    val data: WebViewDataArgs?,
    val reloadAfterConnectionError: Boolean,
    val changeTitleByState: Boolean,
    val viewUrlStrategy: ExternalViewUrlStrategy
): Serializable {

    fun buildUpon() = Builder()
        .setTitle(title)
        .setUrl(url)
        .setData(data)
        .setReloadAfterConnectionError(reloadAfterConnectionError)
        .setChangeTitleOnLoad(changeTitleByState)
        .setViewUrlStrategy(viewUrlStrategy)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebViewCustomizer) return false

        if (title != other.title) return false
        if (url != other.url) return false
        if (data != other.data) return false
        if (reloadAfterConnectionError != other.reloadAfterConnectionError) return false
        if (changeTitleByState != other.changeTitleByState) return false
        if (viewUrlStrategy != other.viewUrlStrategy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        result = 31 * result + reloadAfterConnectionError.hashCode()
        result = 31 * result + changeTitleByState.hashCode()
        result = 31 * result + viewUrlStrategy.hashCode()
        return result
    }

    override fun toString(): String {
        return "WebViewCustomizer(title='$title'," +
                "url='$url'," +
                "data=$data, reloadAfterConnectionError=$reloadAfterConnectionError," +
                "changeTitleOnLoad=$changeTitleByState," +
                "strategy=$viewUrlStrategy)"
    }

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

        /**
         * Выставить [url] с любой валидной схемой или [URL_PAGE_BLANK]
         */
        fun setUrl(url: String?): Builder {
            this.url = url.takeIf {
                it.isUrlValid(orBlank = true, isNonResource = false)
            }?.toString().orEmpty()
            return this
        }

        /**
         * Выставить готовую [uri] без проверок
         */
        fun setUri(uri: Uri) : Builder {
            this.url = uri.toString()
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

        fun setViewUrlStrategy(strategy: ExternalViewUrlStrategy): Builder {
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

        /**
         * Соответствие текущей урлы по возможным
         * схемам / хостам / параметрам
         */
        data class UrlMatch(
            val schemes: List<String>,
            val hosts: List<String>,
            val queryParameters: List<String>,
        ): ExternalViewUrlStrategy() {

            fun match(uri: Uri): Boolean {
                val schemePassed = if (schemes.any { it.isNotEmpty() }) {
                    schemes.any {
                        uri.scheme?.equals(it) == true
                    }
                } else {
                    true
                }
                val hostPassed = if (hosts.any { it.isNotEmpty() }) {
                    hosts.any {
                        uri.host.equalsIgnoreSubDomain(it)
                    }
                } else {
                    true
                }
                val parametersPassed = if (queryParameters.any { it.isNotEmpty() }) {
                    uri.queryParameterNames.any { name ->
                        queryParameters.any { name.equals(it, true) }
                    }
                } else {
                    true
                }
                return schemePassed && hostPassed && parametersPassed
            }
        }

        data object NonBrowserFirst : ExternalViewUrlStrategy() {

            private fun readResolve(): Any = NonBrowserFirst
        }
    }
}