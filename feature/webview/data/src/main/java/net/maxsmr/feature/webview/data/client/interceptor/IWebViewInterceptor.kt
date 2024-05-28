package net.maxsmr.feature.webview.data.client.interceptor

import android.webkit.WebResourceResponse
import net.maxsmr.core.android.content.FileFormat
import java.io.ByteArrayInputStream
import java.io.Serializable

interface IWebViewInterceptor : Serializable {

    fun shouldIntercept(url: String, interceptCondition: (String) -> InterceptedUrl?): InterceptedUrl?

    fun getStubForInterceptedRequest() = WebResourceResponse(
        FileFormat.HTML.mimeType,
        Charsets.UTF_8.name(),
        ByteArrayInputStream(ByteArray(0))
    )

    data class InterceptedUrl(val type: Type, val url: String?) {
        enum class Type {
            OK,
            DECLINE,
            CANCEL
        }
    }
}