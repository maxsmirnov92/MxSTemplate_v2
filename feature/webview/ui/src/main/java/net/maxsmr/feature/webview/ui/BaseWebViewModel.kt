package net.maxsmr.feature.webview.ui

import android.net.Uri
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient.WebViewData
import net.maxsmr.feature.webview.data.client.exception.WebResourceException
import net.maxsmr.feature.webview.ui.BaseWebViewModel.MainWebViewData.Companion.fromWebViewData

open class BaseWebViewModel(state: SavedStateHandle) : BaseHandleableViewModel(state) {

    /**
     * Первые данные в WebView с состоянием загрузки/ошибки - после очередного вызова loadUrl/loadData
     */
    private val _firstWebViewData = MutableLiveData<LoadState<MainWebViewData?>>(LoadState.success(null))

    val firstWebViewData = _firstWebViewData as LiveData<LoadState<MainWebViewData?>>

    /**
     * Текущие данные в [WebView] с состоянием загрузки/ошибки
     */
    private val _currentWebViewData = MutableLiveData<LoadState<MainWebViewData?>>(LoadState.success(null))

    val currentWebViewData = _currentWebViewData as LiveData<LoadState<MainWebViewData?>>

    private val _currentWebViewProgress = MutableLiveData<Int?>(null)

    val currentWebViewProgress = _currentWebViewProgress as LiveData<Int?>

    val currentUrl = currentWebViewData.map { it.data?.url }

    val currentData = currentWebViewData.map { it.data?.data }

    /**
     * Был ли выставлен завершённый [firstWebViewData]
     * с момента создания [BaseWebViewModel]
     */
    private var isFirstResourceChanged = false

    @CallSuper
    open fun onWebViewReload() {
        isFirstResourceChanged = false
    }

    @CallSuper
    open fun onWebViewDestroyed() {
        _firstWebViewData.value = LoadState.success(null)
        _currentWebViewData.value = LoadState.success(null)
        _currentWebViewProgress.value = null
        isFirstResourceChanged = false
    }

    /**
     * Вызывается из [WebView]-колбеков и меняет состояние LD с [WebViewData]
     * относ-но текущего [isFirstResourceChanged] на VM с момента создания
     */
    fun notifyResourceChanged(resource: LoadState<WebViewData>, title: String? = null) {
        val isForMainFrame = resource.data?.isForMainFrame == true
        if (!isForMainFrame) {
            // игнор ресурсов, не относящихся к главной странице / без WebViewData вовсе
            return
        }
        @Suppress("UNCHECKED_CAST")
        val thisData = resource.copyOf(
            fromWebViewData(resource.data, title)
        ) as LoadState<MainWebViewData?>

        val shouldChangeFirst = !isFirstResourceChanged
        if (shouldChangeFirst) {
            // переприсвоение, только если он является loading/завершённым
            // относительно последнего loadUrl/loadData !
            _firstWebViewData.value = thisData
        }
        _currentWebViewData.value = thisData
        if (!resource.isLoading) {
            // завершённое состояние ->
            // последующие вызовы не будут менять firstWebDataResource
            // до следующего loadUrl/loadData
            isFirstResourceChanged = true
        }
    }

    fun onFirstLoadNotStarted(exception: WebResourceException, url: Uri?, data: String?) {
        onWebViewReload()
        notifyResourceChanged(
            LoadState.error(
                exception, if (url != null && !data.isNullOrEmpty()) {
                    WebViewData.fromUrlWithData(url, data)
                } else if (url != null) {
                    WebViewData.fromUrl(url)
                } else {
                    WebViewData.fromData(data)
                }
            )
        )
    }

    fun onProgressChanged(progress: Int) {
        _currentWebViewProgress.value = if (progress in 0..99 /*&& firstWebViewData.value?.isLoading == true*/) {
            progress
        } else {
            null
        }
    }

    /**
     * [WebViewData] только для isMainFrame=true
     * [url] исходный URL, по которому была инициализирована загрузка через loadUrl, или текущий
     * [data] исходные данные для загрузки с [url] или без
     */
    data class MainWebViewData(
        val url: Uri?,
        val data: String?,
        val title: String?,
        val response: WebResourceResponse? = null,
        val responseData: String? = null,
    ) {

        val isEmpty = url == null && data.isNullOrEmpty()

        companion object {

            @JvmStatic
            fun fromWebViewData(
                data: WebViewData?,
                title: String? = null
            ): MainWebViewData? =
                data?.let {
                    MainWebViewData(it.url, it.data, title, it.response, it.responseData)
                }
        }
    }
}