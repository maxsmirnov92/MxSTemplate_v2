package net.maxsmr.feature.webview.ui

import android.net.Uri
import android.text.TextUtils
import net.maxsmr.commonutils.CHARSET_DEFAULT
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.content.FileFormat
import java.io.Serializable

/**
 * Инкапсулирет задание свойств для [android.webkit.WebView]
 */
class WebViewCustomizer private constructor(
    val title: String,
    val url: String,
    val data: WebViewDataArgs?,
    val canInputUrls: Boolean,
    // TODO заменить на отдельные части урлы
    val queryParameters: List<String>,
): Serializable {

    fun buildUpon() = Builder()
        .setTitle(title)
        .setUrl(url)
        .setData(data)
        .setCanInputUrls(canInputUrls)
        .setOpenSystemBrowserByQueryParameterNames(queryParameters)

    class Builder {

        private var title: String = EMPTY_STRING
        private var url: String  = EMPTY_STRING
        private var data: WebViewDataArgs? = null
        private var canInputUrls: Boolean = false
        private val queryParameters = mutableListOf<String>()

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

        fun setCanInputUrls(toggle: Boolean): Builder {
            this.canInputUrls = toggle
            return this
        }

        fun setOpenSystemBrowserByQueryParameterNames(names: List<String>): Builder {
            this.queryParameters.clear()
            this.queryParameters.addAll(names)
            return this
        }

        fun build() = WebViewCustomizer(title, url, data, canInputUrls, queryParameters)
    }

    data class WebViewDataArgs @JvmOverloads constructor(
        val data: String,
        val mimeType: String = FileFormat.HTML.mimeType,
        val charset: String = CHARSET_DEFAULT,
        val forceBase64: Boolean = true
    ): Serializable
}