package net.maxsmr.feature.webview.data.client

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.text.TextUtils
import android.webkit.*
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import net.maxsmr.commonutils.URL_SCHEME_GEO
import net.maxsmr.commonutils.URL_SCHEME_GEO_GOOGLE
import net.maxsmr.commonutils.URL_SCHEME_MAIL
import net.maxsmr.commonutils.URL_SCHEME_MARKET
import net.maxsmr.commonutils.URL_SCHEME_TEL
import net.maxsmr.commonutils.getDialIntent
import net.maxsmr.commonutils.getViewLocationIntent
import net.maxsmr.commonutils.getViewUrlIntent
import net.maxsmr.commonutils.isAtLeastNougat
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.media.getMimeTypeFromUrl
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.network.asStringCloned
import net.maxsmr.core.network.exceptions.HttpProtocolException
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.executeCall
import net.maxsmr.core.network.getContentTypeHeader
import net.maxsmr.core.network.toPairs
import net.maxsmr.core.ui.openAnyIntentWithToastError
import net.maxsmr.core.ui.openEmailIntentWithToastError
import net.maxsmr.feature.webview.data.client.exception.WebResourceException
import net.maxsmr.feature.webview.data.client.exception.WebResourceSslException
import net.maxsmr.feature.webview.data.client.interceptor.IWebViewInterceptor
import net.maxsmr.feature.webview.data.client.interceptor.IWebViewInterceptor.InterceptedUrl
import net.maxsmr.feature.webview.data.client.interceptor.WebViewInterceptor
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Response
import java.nio.charset.Charset

/**
 * @param okHttpClient применим только для api >= 21
 */
open class InterceptWebViewClient @JvmOverloads constructor(
    protected val context: Context,
    protected val okHttpClient: OkHttpClient? = null,
    private val webViewInterceptor: IWebViewInterceptor? = WebViewInterceptor(),
) : WebViewClient() {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    /**
     * Нужен для последующих вызовов [onWebResourceRequestError] и/или [onPageFinished];
     * + индикатор текущей загрузки основной страницы
     */
    var currentMainFrameData: WebViewData? = null
        private set(value) {
            if (value != null && !value.isForMainFrame) {
                throw IllegalArgumentException("Setting WebViewData that is not for main frame")
            }
            field = value
        }

    var onPageStarted: ((WebViewData) -> Unit)? = null

    var onPageFinished: ((WebViewData) -> Unit)? = null

    var onPageError: ((WebViewData, NetworkException) -> Unit)? = null

    // вызовется при < 21 api
    @CallSuper
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        // isForMainFrame - определить невозможно, executeCall выполняться не будет, т.к. неизвестен метод
        return shouldInterceptRequest(view, url, null, null, true)
    }

    @CallSuper
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return shouldInterceptRequest(
            view,
            request.url.toString(),
            request.method,
            request.requestHeaders,
            request.isForMainFrame
        )
    }

    // вызывается после onPageStarted
    private fun shouldInterceptRequest(
        view: WebView,
        url: String,
        method: String?,
        headers: Map<String, String>?,
        isForMainFrame: Boolean,
    ): WebResourceResponse? {
        logger.d("shouldInterceptRequest, url: $url, method: $method, headers: $headers, isForMainFrame: $isForMainFrame")
        onStartLoading(url, isForMainFrame)
        return if (shouldInterceptCommand(view, url, ::shouldInterceptFromRequest)) {
            webViewInterceptor?.getStubForInterceptedRequest()
        } else {
            val response: Response? = if (method == Method.GET.value) {
                // выполняем вручную только геты:
                // (для POST, например, отсутствует тело - Request.Builder бросит исключение)
                okHttpClient?.executeCall { builder ->
                    builder.url(url)
                    builder.method(method, null)
                    headers?.entries?.forEach {
                        builder.addHeader(it.key, it.value)
                    }
                }
            } else {
                null
            }
            val data = response.toWebViewData(url, isForMainFrame)
            onFinishLoading(data, isForMainFrame)
            // FIXME при возврате ненульного респонса по некоторым урлам возможны циклические редиректы
            data.response
        }
    }

    // вызовется при < 21 api
    @CallSuper
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return shouldInterceptCommand(view, url, ::shouldInterceptFromOverrideUrlWithCheck)
    }

    @CallSuper
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return shouldInterceptCommand(view, request.url.toString(), ::shouldInterceptFromOverrideUrlWithCheck)
    }

    @MainThread
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        logger.d("onPageStarted, url: $url")
    }

    @MainThread
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        logger.d("onPageFinished, url: $url")
        val uri = url.toUri()
        val lastData = currentMainFrameData
        var isHandled = false
        if (lastData != null) {
            // проверку пришлось убрать из-за того, что при редиректах приходит завершение по другой урле
            /*&& lastData.url != null && uri.equalsIgnoreSubDomain(lastData.url)*/

            // загрузка завершилась для текущей WebViewData;
            // как индикатор завершения текущей цепочки - забываем
            onPageFinished?.invoke(
                lastData.copy(
                    url = uri,
                    data = null
                )
            ) // актуализация урлы, т.к. это может быть редирект
            currentMainFrameData = null
            isHandled = true
        }
        if (!isHandled) {
            // если урла не совпала
            // или currentMainFrameData отсутствует (главная была завершена)
            // -> это загрузка какой-то части страницы, просто оповещаем
            onPageFinished?.invoke(WebViewData(uri, null, false))
        }
    }

    // вызовется при < 23 api
    override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        onWebResourceRequestError(WebResourceException(code = errorCode, message = description.toString()), null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @CallSuper
    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        onWebResourceRequestError(
            WebResourceException(
                code = error.errorCode,
                message = error.description.toString()
            ), request
        )
    }

    @CallSuper
    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        onWebResourceRequestError(
            HttpProtocolException(
                request.url.toString(),
                request.method,
                ArrayList(request.requestHeaders.toHeaders().toPairs()),
                responseCode = errorResponse.statusCode,
                responseMessage = errorResponse.reasonPhrase,
                responseBodyHeaders = ArrayList(errorResponse.responseHeaders.toHeaders().toPairs())
            ), request
        )
        // далее последует onPageFinished
    }

    @CallSuper
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        super.onReceivedSslError(view, handler, error)
        onWebResourceRequestError(WebResourceSslException(error), null)
        // onPageFinished не вызовется после
    }

    @CallSuper
    @Synchronized
    open fun onWebResourceRequestError(error: NetworkException, request: WebResourceRequest?) {
        logger.e("onWebResourceRequestError, error: $error, request: ${request?.toReadableString()}")
        val data = currentMainFrameData
        if (data != null && request == null) {
            // если WebResourceRequest отсутствует (при ошибке SSL)
            // -> оповещаем об ошибке с существующей currentMainFrameData
            onPageError?.invoke(data, error)
            // забываем, т.к. onPageFinished дальше не вызовется
            currentMainFrameData = null
        } else if (request != null) {
            // иначе просто оповещаем об ошибке, оставляя currentMainFrameData,
            // чтобы далее правильно вызвался onPageFinished
            val isForMainFrame = request.isForMainFrame
            val url = request.url
            val newData = if (data != null && isForMainFrame) {
                // можно идентифицировать как главный, начатый ранее
                data.copy(url = url)
            } else {
                // такого быть не должно, т.к. должен был быть запомнен currentMainFrameData при старте
                WebViewData(url, null, isForMainFrame)
            }.also {
                if (isForMainFrame) {
                    currentMainFrameData = it
                }
            }
            onPageError?.invoke(newData, error)
        }
    }

    open fun shouldInterceptFromRequest(url: String): InterceptedUrl? = null

    open fun shouldInterceptFromOverrideUrl(url: String): InterceptedUrl? = null

    protected open fun onUrlIntercepted(view: WebView, interceptedUrlType: InterceptedUrl) {}

    // не главный поток
    @CallSuper
    protected open fun onStartLoading(url: String, isForMainFrame: Boolean) {
        synchronized(this) {
            val data = WebViewData(url.toUri(), null, isForMainFrame)
            if (isForMainFrame) {
                currentMainFrameData = data
            }
            onPageStarted?.invoke(data)
        }
    }

    // не главный поток
    @CallSuper
    protected open fun onFinishLoading(webViewData: WebViewData, isForMainFrame: Boolean) {
        synchronized(this) {
            if (isForMainFrame) {
                currentMainFrameData = webViewData
            }
            // далее ожидаем еррор + onPageFinished
            // или только onPageFinished
            // или только onReceivedSslError
        }
    }

    private fun shouldInterceptCommand(
        view: WebView,
        url: String,
        interceptedFunc: (String) -> InterceptedUrl?,
    ): Boolean {
        val interceptedUrlType = webViewInterceptor?.shouldIntercept(url) { _url -> interceptedFunc(_url) }
        return if (interceptedUrlType != null) {
            // первый или повторный перехват урлы в зав-ти от реализации webViewInterceptor
            onUrlIntercepted(view, interceptedUrlType)
            true
        } else {
            false
        }
    }

    /**
     * Срабатывает при редиректах по страницам, например при клике по ссылке;
     * затем идёт [shouldInterceptRequest]
     */
    private fun shouldInterceptFromOverrideUrlWithCheck(url: String): InterceptedUrl? {
        val uri = url.toUri()
        val scheme = uri.scheme
        var handled = false
        when (scheme) {
            URL_SCHEME_MAIL -> {
                handled = context.openEmailIntentWithToastError(uri)
            }

            URL_SCHEME_TEL -> {
                getDialIntent(uri)?.let {
                    handled = context.openAnyIntentWithToastError(it)
                }
            }

            URL_SCHEME_GEO, URL_SCHEME_GEO_GOOGLE -> {
                handled = context.openAnyIntentWithToastError(
                    getViewUrlIntent(uri, null),
                    errorResId = net.maxsmr.core.ui.R.string.error_intent_open_geo
                )
            }

            URL_SCHEME_MARKET -> {
                handled = context.openAnyIntentWithToastError(
                    getViewUrlIntent(uri, null),
                    errorResId = net.maxsmr.core.ui.R.string.error_intent_open_market
                )
            }
            // перебирать остальные известные не http/https схемы
            // для открытия аппов напрямую,
            // не полагаясь на обработку NON_BROWSER, доступную начиная с S
        }
        return if (handled) {
            InterceptedUrl(InterceptedUrl.Type.OK, url)
        } else {
            shouldInterceptFromOverrideUrl(url)
        }
    }

    private fun Response?.toWebViewData(url: String, isForMainFrame: Boolean): WebViewData {
        var responseBody: String? = null
        val response = this?.let {

            val mimeType = getContentTypeHeader()
            val headers = mutableMapOf<String, String>()

            // копирование тела ответа для дальнейшего переиспользования в WebView
            val bodyWithCharset = it.asStringCloned()
            val charset = bodyWithCharset?.second ?: Charset.defaultCharset()
            responseBody = bodyWithCharset?.first

            this.headers.forEach { p -> headers[p.first] = p.second }

            WebResourceResponse(
                mimeType.takeIf { type -> !TextUtils.isEmpty(type) }
                    ?: getMimeTypeFromUrl(url).takeIf { !TextUtils.isEmpty(it) }
                    ?: FileFormat.HTML.mimeType,
                charset.name(),
                code,
                // бросает exception при пустом
                message.takeIf { mes -> mes.isNotEmpty() } ?: "OK",
                headers,
                body?.byteStream()
            )
        }
        return WebViewData(url.toUri(), null, isForMainFrame, response, responseBody)
    }

    /**
     * [url] или [data] исходный url или данные (в зав-ти от loadUrl и loadData и последующих вызовов)
     * [response] и [responseData] нульные, если запрос не выполнялся вручную в Interceptor
     * (или был exception в процессе executeCall)
     */
    data class WebViewData(
        val url: Uri?,
        val data: String?,
        val isForMainFrame: Boolean,
        val response: WebResourceResponse? = null,
        val responseData: String? = null,
    ) {

        companion object {

            @JvmStatic
            fun fromUrlWithData(url: Uri?, data: String?) =
                if (url != null && !url.scheme.isNullOrEmpty() && !data.isNullOrEmpty()) {
                    WebViewData(url, data, true, null, null)
                } else {
                    null
                }

            @JvmStatic
            fun fromUrl(url: Uri?) = if (url != null && !url.scheme.isNullOrEmpty()) {
                WebViewData(url, null, true, null, null)
            } else {
                null
            }

            @JvmStatic
            fun fromData(data: String?) = if (!data.isNullOrEmpty()) {
                WebViewData(null, data, true, null, null)
            } else {
                null
            }
        }
    }

    companion object {

        fun WebResourceRequest.toReadableString(): String {
            return "WebResourceRequest(url='$url', isForMainFrame='$isForMainFrame', isRedirect=${
                if (isAtLeastNougat()) {
                    isRedirect
                } else {
                    null
                }
            }, hasGesture=${hasGesture()}, method=$method)"
        }
    }
}