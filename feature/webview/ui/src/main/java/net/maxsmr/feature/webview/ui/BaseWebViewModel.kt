package net.maxsmr.feature.webview.ui

import android.webkit.WebView
import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient.WebViewData
import net.maxsmr.feature.webview.data.client.exception.WebResourceException

open class BaseWebViewModel(state: SavedStateHandle) : BaseHandleableViewModel(state) {

    /**
     * Первые данные в WebView с состоянием загрузки/ошибки - после очередного вызова loadUrl/loadData
     */
    private val _firstWebViewData = MutableLiveData<LoadState<WebViewData?>>(LoadState.success(null))

    val firstWebViewData = _firstWebViewData as LiveData<LoadState<WebViewData?>>

    /**
     * Текущие данные в [WebView] с состоянием загрузки/ошибки
     * + флаг о том является ли он loading/завершённым относительно последнего вызова loadUrl/loadData
     */
    private val _currentWebViewData = MutableLiveData<LoadState<WebViewData?>>(LoadState.success(null))

    val currentWebViewData = _currentWebViewData as LiveData<LoadState<WebViewData?>>

    private val _currentWebViewProgress = MutableLiveData<Int?>(null)

    val currentWebViewProgress = _currentWebViewProgress as LiveData<Int?>

    protected val currentUrl get() = currentWebViewData.value?.data?.url?.takeIf { it.isNotEmpty() }

    /**
     * Был ли выставлен завершённый [firstWebViewData]
     * с момента создания [BaseWebViewModel]
     */
    protected var isFirstResourceChanged = false
        private set

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
    @Suppress("UNCHECKED_CAST")
    fun applyResource(resource: LoadState<WebViewData>) {
        val isForMainFrame = resource.data?.isForMainFrame == true
        if (!isForMainFrame) {
            // игнор ресурсов, не относящихся к главной странице / без WebViewData вовсе
            return
        }
        val shouldChangeFirst = !isFirstResourceChanged
        if (shouldChangeFirst) {
            // переприсвоение, только если он является loading/завершённым
            // относительно последнего loadUrl/loadData !
            _firstWebViewData.postValue(resource as LoadState<WebViewData?>)
        }
        _currentWebViewData.postValue(resource as LoadState<WebViewData?>)
        if (!resource.isLoading) {
            // завершённое состояние ->
            // последующие вызовы не будут менять firstWebDataResource
            // до следующего loadUrl/loadData
            isFirstResourceChanged = true
        }
    }

    fun onFirstLoadNotStarted(exception: WebResourceException, url: String) {
        _firstWebViewData.value = LoadState.error(exception, WebViewData.fromMainUrl(url))
    }

    fun onProgressChanged(progress: Int) {
        _currentWebViewProgress.value = if (progress in 0..99 /*&& firstWebViewData.value?.isLoading == true*/) {
            progress
        } else {
            null
        }
    }
}