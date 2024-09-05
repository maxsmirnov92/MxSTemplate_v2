package net.maxsmr.feature.webview.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.webkit.WebBackForwardList
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.annotation.CallSuper
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.maxsmr.commonutils.CHARSET_DEFAULT
import net.maxsmr.commonutils.gui.BaseUrlParams
import net.maxsmr.commonutils.gui.loadDataCompat
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.android.network.URL_PAGE_BLANK
import net.maxsmr.core.android.network.isUrlValid
import net.maxsmr.core.network.exceptions.HttpProtocolException
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.isResponseOk
import net.maxsmr.core.network.toPairs
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient.WebViewData
import net.maxsmr.feature.webview.data.client.ProgressWebChromeClient
import net.maxsmr.feature.webview.data.client.exception.EmptyWebResourceException
import net.maxsmr.feature.webview.data.client.exception.WebResourceException
import okhttp3.Headers.Companion.toHeaders
import java.nio.charset.Charset

abstract class BaseWebViewFragment<VM : BaseWebViewModel> : BaseNavigationFragment<VM>() {

    abstract val webView: WebView

    abstract val swipeRefresh: SwipeRefreshLayout?

    abstract val progress: ProgressBar?

    abstract val errorContainer: View?

    override val connectionHandler: ConnectionHandler = ConnectionHandler.Builder()
        .onStateChanged {
            if (it && shouldReloadAfterConnectionError) {
                val data = viewModel.currentWebViewData.value
                data?.error?.let { error ->
                    if (error is WebResourceException && error.isConnectionError) {
                        // с задержкой, т.к. после появления сети коннект может не пройти сразу
                        webView.postDelayed({
                            // если последняя ошибка была обусловлена сетью,
                            // загрузить повторно при появлении сети сейчас
                            doReloadWebView()
                        }, 500)
                    }
                }
            }
        }
        .build()

    protected open val shouldReloadAfterConnectionError: Boolean = true

    protected open val shouldInterceptOnUpPressed: Boolean = false

    protected open val shouldInterceptOnBackPressed: Boolean = true

    protected var isWebViewInitialized = false
        private set

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        setupWebView(savedInstanceState)
        viewModel.firstWebViewData.observe {
            onFirstResourceChanged(it)
        }
        viewModel.currentWebViewData.observe {
            onResourceChanged(it)
        }
        viewModel.currentWebViewProgress.observe {
            if (it != null) {
                onShowProgress(it)
            } else {
                onHideProgress()
            }
        }
        swipeRefresh?.setOnRefreshListener {
            doReloadWebView()
        }
        swipeRefresh?.let {
            it.viewTreeObserver?.addOnScrollChangedListener {
                it.isEnabled = webView.scrollY == 0
            }
        }

        webView.evaluateJavascript(
            "(function() { return !!window.WebGLRenderingContext && !!document.createElement('canvas').getContext('webgl'); })();"
        ) { value ->
            if ("true" == value) {
                Log.d("WebView", "WebGL поддерживается")
            } else {
                Log.d("WebView", "WebGL не поддерживается")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isRemoving || !isAdded) return
        val bundle = Bundle()
        webView.saveState(bundle)
        outState.putBundle(ARG_WEB_VIEW_STATE, bundle)
    }

    override fun onUpPressed(): Boolean = if (shouldInterceptOnUpPressed && webView.canGoBack()) {
        webView.goBack()
        true
    } else {
        super.onUpPressed()
    }

    override fun onBackPressed(): Boolean = if (shouldInterceptOnBackPressed && webView.canGoBack()) {
        webView.goBack()
        true
    } else {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyWebView()
    }

    /**
     * Перезагрузка исходными данными
     */
    protected abstract fun doInitReloadWebView()

    protected abstract fun createWebViewClient(): InterceptWebViewClient

    protected open fun createWebChromeClient(): ProgressWebChromeClient? = ProgressWebChromeClient()

    /**
     * Перезагрузка с текущим состоянием WebView
     */
    @CallSuper
    protected open fun doReloadWebView() {
//        viewModel.onWebViewReload()
        webView.reload()
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected open fun onSetupWebView(webSettings: WebSettings) {
        with(webSettings) {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            displayZoomControls = false
            builtInZoomControls = true
            defaultTextEncodingName = CHARSET_DEFAULT
            allowContentAccess = true
            allowFileAccess = true
        }
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    @CallSuper
    protected open fun onDestroyWebView() {
        logger.d("onDestroyWebView")
        webView.destroy()
        viewModel.onWebViewDestroyed()
    }

    @CallSuper
    protected open fun onPageStarted(data: WebViewData) {
        logger.d("onPageStarted, data: '$data")
        if (!data.isForMainFrame) {
            return
        }
        LoadState.loading(data).notifyChanged()
    }

    /**
     * Сработает при любой ошибке;
     * Иметь в виду, что при ошибке SSL далее не последует [onPageFinished]
     */
    @CallSuper
    protected open fun onPageError(data: WebViewData, exception: NetworkException) {
        logger.e("onPageError, data: '$data', exception: '$exception'")
        if (!data.isForMainFrame) {
            return
        }
        LoadState.error(exception, data).notifyChanged()
    }

    /**
     * Колбек сработает в результате загрузки страницы:
     * 1. Успех
     * 2. Ошибка, по которой вызвался [onPageError], кроме SSL
     * По некоторым страницам почему-то не приходит
     */
    @CallSuper
    protected open fun onPageFinished(data: WebViewData) {
        logger.d("onPageFinished, data:'$data'")
        if (!data.isForMainFrame) {
            return
        }
        webView.post {
            with(viewModel) {
                // используем инфу о состоянии для последнего ресурса с той же урлой
                val currentResource = currentWebViewData.value
                (if (currentResource != null && currentResource.isError()) {
                    // если до этого в onPageLoadError выставлялся еррор - оставляем статус
                    currentResource.copyOf(data)
                } else {
                    // в случае успеха (перед ним был loading) предоставляем возможность отдельно проанализировать данные респонса
                    hasResponseError(data)?.let {
                        LoadState.error(it, data)
                    } ?: LoadState.success(data)
                }).notifyChanged(false)
            }
        }
    }

    /**
     * Вызовется, если восстановление из [Bundle] было успешным;
     * На этом моменте можно обратиться к url в самой [WebView] или к [BaseWebViewModel.firstWebViewData] -
     * они должны быть идентичными;
     * При этом [onPageStarted] сработает далее
     */
    @CallSuper
    protected open fun onWebViewStateRestored(list: WebBackForwardList) {
        logger.d("onWebViewStateRestored")
    }

    @CallSuper
    protected open fun onWebViewFirstInit() {
        logger.d("onWebViewFirstInit")
        doInitReloadWebView()
    }

    /**
     * Вызывается при загрузке,
     * инициированной последним [loadUri]/[loadData]
     */
    protected open fun onFirstResourceChanged(resource: LoadState<BaseWebViewModel.MainWebViewData?>) {}

    /**
     * При любых mainframe лоадингах (например при навигациях по странице)
     */
    @CallSuper
    protected open fun onResourceChanged(resource: LoadState<BaseWebViewModel.MainWebViewData?>) {
        val url = resource.data?.url
        val data = resource.data?.data
        val hasData = resource.hasData { it != null && !it.isEmpty }
        if (resource.isLoading) {
            webView.isVisible = hasData
            progress?.isIndeterminate = viewModel.currentWebViewProgress.value == null
            progress?.isVisible = true
            errorContainer?.isVisible = false
            onResourceLoading(url, data)
        } else {
            progress?.isVisible = false
            swipeRefresh?.isRefreshing = false
            if (resource.isSuccess() && hasData) {
                swipeRefresh?.isEnabled = true
                webView.isVisible = true
                errorContainer?.isVisible = false
                onResourceSuccess(url, data)
            } else {
                val webResourceException = resource.error as? WebResourceException
                val shouldShowError = resource.isError()
                        // не отображаем стейт с ошибкой, если это http-ошибка (>400),
                        // и должно иметь свою обработку в WebView
                        // (а если контент отсутвует - заглушки на этот случай нет)
                        && webResourceException != null
                        && webResourceException !is EmptyWebResourceException
                // свайп доступен только при видимой webview
                swipeRefresh?.isEnabled = !shouldShowError
                webView.isVisible = !shouldShowError
                errorContainer?.isVisible = shouldShowError
                if (shouldShowError) {
                    onResourceError(hasData, url, data, webResourceException as WebResourceException)
                }
            }
        }
    }

    protected open fun onResourceLoading(url: Uri?, data: String?) {}

    protected open fun onResourceSuccess(url: Uri?, data: String?) {}

    /**
     * Для дополнительной логики при ерроре
     */
    protected open fun onResourceError(hasData: Boolean, url: Uri?, data: String?, exception: WebResourceException) {}

    protected open fun onShowProgress(progress: Int) {
        logger.d("onShowProgress, progress: $progress")
        this.progress?.isIndeterminate = false
        this.progress?.progress = progress
        this.progress?.isVisible = true
    }

    protected open fun onHideProgress() {
        logger.d("onHideProgress")
        this.progress?.isVisible = false
    }

    protected fun loadUrl(url: String) {
        // toValidUri можно не использовать, т.к. дальше проверяется валидность у строки
        loadUri(url.toUri())
    }

    protected fun loadUri(uri: Uri): Boolean {
        logger.d("loadUrl, url: '$uri'")
        with(viewModel) {
            val uriString = uri.toString()
            return if (isWebViewInitialized && uriString.isUrlValid(orBlank = true, isNonResource = false)) {
                webView.loadUrl(uriString)
                if (uriString.equals(URL_PAGE_BLANK, true)) {
                    // по about:blank колбеков нет
                    onPageFinished(WebViewData(uri, null, true))
                }
                true
            } else {
                onFirstLoadNotStarted(EmptyWebResourceException(), uri, null)
                false
            }
        }
    }

    /**
     * @param mimeType тип данных для [data]
     * @param checkEmpty при false и загрузке пустых исходных данных будет успех (стандартное поведение WebView);
     * true, если требуется еррор в кач-ве результата, когда предполагается заглушка (по умолчанию)
     */
    @JvmOverloads
    protected fun loadData(
        data: String,
        mimeType: String = FileFormat.HTML.mimeType,
        charset: Charset = Charset.defaultCharset(),
        forceBase64: Boolean = true,
    ): Boolean {
        logger.d("loadUrl, data: '$data', mimeType: '$mimeType', charset: '$charset', forceBase64: '$forceBase64'")
        with(viewModel) {
            return if (isWebViewInitialized && data.isNotEmpty()) {
                webView.loadDataCompat(
                    data,
                    mimeType,
                    charset,
                    null,
                    forceBase64
                )
                true
            } else {
                onFirstLoadNotStarted(EmptyWebResourceException(), null, data)
                false
            }
        }
    }

    @JvmOverloads
    protected fun loadDataWithBaseUrl(
        baseUrl: String?,
        data: String,
        mimeType: String = FileFormat.HTML.mimeType,
        charset: Charset = Charset.defaultCharset(),
        failUrl: String? = null,
        forceBase64: Boolean = true,
    ): Boolean {
        logger.d("loadDataWithBaseUrl, baseUrl: '$baseUrl', data: '$data', mimeType: '$mimeType', charset: '$charset', forceBase64: '$forceBase64'")
        with(viewModel) {
            return if (isWebViewInitialized && data.isNotEmpty()) {
                webView.loadDataCompat(
                    data,
                    mimeType,
                    charset,
                    BaseUrlParams(baseUrl, failUrl),
                    forceBase64
                )
                true
            } else {
                onFirstLoadNotStarted(EmptyWebResourceException(), baseUrl?.toUri(), data)
                false
            }
        }
    }

    protected open fun hasResponseError(data: WebViewData?): NetworkException? {
        var result: NetworkException? = null
        data?.response?.let { response ->
            val url = data.url?.toString()
            val code = response.statusCode
            // перестраховка, не должны попасть сюда с еррорным http-кодом
            if (url != null && !isResponseOk(code)) {
                // request здесь отсутствует
                result = HttpProtocolException(
                    url,
                    EMPTY_STRING,
                    responseCode = code,
                    responseMessage = response.reasonPhrase,
                    responseBodyHeaders = ArrayList(response.responseHeaders.toHeaders().toPairs())
                )
            } else {
                // пустой запрос также считается ошибкой
                if (TextUtils.isEmpty(data.responseData)) {
                    result = EmptyWebResourceException()
                } else {
                    val mimePart = response.mimeType.split(";").getOrNull(0).orEmpty()
                    if (mimePart != FileFormat.HTML.mimeType) {
                        if (!shouldAcceptNonHtmlData(data.responseData)) {
                            result = WebResourceException(code = WebViewClient.ERROR_UNKNOWN)
                        }
                    }
                }
            }
        }
        // при отсутствии response запрос скорее всего не перехватывался - не ошибка
        return result
    }

    protected open fun shouldAcceptNonHtmlData(responseData: String?) = true

    private fun LoadState<WebViewData>.notifyChanged(shouldPost: Boolean = true) {
        fun notifyResourceChanged() {
            viewModel.notifyResourceChanged(this, webView.title)
        }
        if (shouldPost) {
            webView.post {
                notifyResourceChanged()
            }
        } else {
            notifyResourceChanged()
        }
    }

    private fun setupWebView(savedInstanceState: Bundle?) {
//        destroyWebView()
        if (isWebViewInitialized) return

        logger.d("Setting up WebView...")
        createWebViewClient().let {
            it.onPageStarted = { data ->
                onPageStarted(data)
            }
            it.onPageError = { data, error ->
                onPageError(data, error)
            }
            it.onPageFinished = { url ->
                onPageFinished(url)
            }
            webView.webViewClient = it
        }
        createWebChromeClient()?.let {
            it.onProgressChanged = { progress ->
                viewModel.onProgressChanged(progress)
            }
            webView.webChromeClient = it
        }
        onSetupWebView(webView.settings)
        isWebViewInitialized = true

        var isSuccessfullyRestored = false
        savedInstanceState?.getBundle(ARG_WEB_VIEW_STATE)?.let {
            webView.restoreState(it)?.let { list ->
                onWebViewStateRestored(list)
                isSuccessfullyRestored = true
            }
        }
        if (!isSuccessfullyRestored) {
            onWebViewFirstInit()
        }
    }

    private fun destroyWebView() {
        if (isWebViewInitialized) {
            onDestroyWebView()
            isWebViewInitialized = false
        }
    }

    companion object {

        private const val ARG_WEB_VIEW_STATE = "web_view_state"
    }
}