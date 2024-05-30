package net.maxsmr.feature.webview.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.WebBackForwardList
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import net.maxsmr.commonutils.CHARSET_DEFAULT
import net.maxsmr.commonutils.gui.BaseUrlParams
import net.maxsmr.commonutils.gui.loadDataCompat
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.android.network.URL_PAGE_BLANK
import net.maxsmr.core.android.network.isUrlValid
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.isResponseOk
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.webview.data.client.BaseWebChromeClient
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient.WebViewData
import net.maxsmr.feature.webview.data.client.exception.EmptyWebResourceException
import net.maxsmr.feature.webview.data.client.exception.WebResourceException
import java.nio.charset.Charset

abstract class BaseWebViewFragment<VM : BaseWebViewModel> : BaseNavigationFragment<VM>() {

    abstract val webView: WebView

    abstract val progress: ProgressBar?

    abstract val emptyErrorContainer: View?

    override val connectionHandler: ConnectionHandler = ConnectionHandler.Builder()
        .onStateChanged {
            if (it && shouldReloadAfterConnectionError) {
                val data = viewModel.firstWebViewData.value
                data?.error?.let { error ->
                    if (error is WebResourceException && error.isConnectionError) {
                        // если последняя ошибка была обусловлена сетью,
                        // загрузить повторно при появлении сети сейчас
                        doReloadWebView()
                    }
                }
            }
        }
        .build()

    protected open val shouldReloadAfterConnectionError: Boolean = false

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
            onResourceChanged(it.first, it.second)
        }
        viewModel.currentWebViewProgress.observe {
            if (it != null) {
                onShowProgress(it)
            } else {
                onHideProgress()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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

    protected abstract fun doReloadWebView()

    protected abstract fun createWebViewClient(): InterceptWebViewClient

    protected open fun createWebChromeClient(): BaseWebChromeClient? = BaseWebChromeClient()

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
        }
    }

    @CallSuper
    protected open fun onDestroyWebView() {
        logger.d("onDestroyWebView")
        webView.destroy()
    }

    @CallSuper
    protected open fun onPageStarted(data: WebViewData) {
        logger.d("onPageStarted, data: '$data")
        if (!data.isForMainFrame) {
            return
        }
        viewModel.applyResource(LoadState.loading(data))
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
        viewModel.applyResource(LoadState.error(exception, data))
    }

    /**
     * Колбек сработает в результате загрузки страницы:
     * 1. Успех
     * 2. Ошибка, по которой вызвался [onPageError], кроме SSL
     */
    @CallSuper
    protected open fun onPageFinished(data: WebViewData) {
        logger.d("onPageFinished, data:'$data'")
        if (!data.isForMainFrame) {
            return
        }
        with(viewModel) {
            // используем инфу о состоянии для последнего ресурса с той же урлой
            val currentResource = currentWebViewData.value?.takeIf { it.first.data?.url == data.url }?.first
            applyResource((if (currentResource != null && currentResource.isError()) {
                // если до этого в onPageLoadError выставлялся еррор - оставляем статус
                currentResource.copyOf(data)
            } else {
                // в случае успеха (перед ним был loading) предоставляем возможность отдельно проанализировать данные респонса
                hasResponseError(data)?.let {
                    LoadState.error(it, data)
                } ?: LoadState.success(data)
            }))
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

    protected open fun onWebViewFirstInit() {
        logger.d("onWebViewFirstInit")
        doReloadWebView()
    }

    /**
     * Вызывается, когда ресурс, инициированный последним [loadUrl] или [loadData],
     * попадает на этот экран впервые
     */
    protected open fun onFirstResourceChanged(resource: LoadState<WebViewData>) {
        if (resource.isLoading) {
            // webview во время загрузки остаётся только при наличии данных
            webView.isVisible = resource.hasData()
            progress?.isIndeterminate = viewModel.currentWebViewProgress.value == null
            progress?.isVisible = true
            emptyErrorContainer?.isVisible = false
        } else {
            progress?.isVisible = false
            if (resource.isSuccessWithData()) {
                webView.isVisible = true
                emptyErrorContainer?.isVisible = false
            } else {
                webView.isVisible = false
                emptyErrorContainer?.isVisible = true
            }
        }
    }

    /**
     * @param isFirst является ли [resource] loading/завершённым
     * относительно последнего вызова loadUrl/loadData
     */
    protected open fun onResourceChanged(resource: LoadState<WebViewData>, isFirst: Boolean) {}

    protected open fun onShowProgress(progress: Int) {
        this.progress?.isIndeterminate = false
        this.progress?.progress = progress
        this.progress?.isVisible = true
    }

    protected open fun onHideProgress() {
        this.progress?.isVisible = false
    }

    protected fun loadUri(uri: Uri) {
        loadUrl(uri.toString())
    }

    protected fun loadUrl(url: String): Boolean {
        logger.d("loadUrl, url: '$url'")
        with(viewModel) {
            return if (isWebViewInitialized && url.isUrlValid(orBlank = true)) {
                webView.loadUrl(url)
                true
            } else {
                onFirstLoadNotStarted(EmptyWebResourceException(), url)
                false
            }.apply {
                onWebViewReload()
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
                onFirstLoadNotStarted(EmptyWebResourceException(), data)
                false
            }.apply {
                onWebViewReload()
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
                onFirstLoadNotStarted(EmptyWebResourceException(), data)
                false
            }.apply {
                onWebViewReload()
            }
        }
    }

    private fun cancelLoading() {
        webView.post {
            webView.stopLoading()
            webView.loadUrl(URL_PAGE_BLANK)
        }
    }

    protected open fun hasResponseError(data: WebViewData?): WebResourceException? {
        var result: WebResourceException? = null
        data?.response?.let { response ->
            val code = response.statusCode
            // перестраховка, не должны попасть сюда с еррорным http-кодом
            if (!isResponseOk(code)) {
                result = WebResourceException(code = code)
            } else {
                val mimePart = response.mimeType.split(";").getOrNull(0).orEmpty()
                if (mimePart != FileFormat.HTML.mimeType) {
                    if (!handleNonHtmlData(data.responseData)) {
                        result = WebResourceException(code = WebViewClient.ERROR_UNKNOWN)
                    }
                } else {
                    // пустой запрос также считается ошибкой
                    if (TextUtils.isEmpty(data.responseData)) {
                        result = EmptyWebResourceException()
                    }
                }
            }
        }
        // при отсутствии response запрос скорее всего не перехватывался - не ошибка
        return result
    }

    protected open fun handleNonHtmlData(responseData: String?) = true

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