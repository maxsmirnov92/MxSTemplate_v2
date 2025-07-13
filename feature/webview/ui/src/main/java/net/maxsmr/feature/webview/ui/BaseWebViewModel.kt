package net.maxsmr.feature.webview.ui

import android.net.Uri
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.states.LoadState.Companion.copyOf
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.feature.webview.data.client.InterceptWebViewClient.WebViewData
import net.maxsmr.feature.webview.data.client.exception.WebResourceException
import net.maxsmr.feature.webview.ui.BaseWebViewModel.MainWebViewData.Companion.fromWebViewData

open class BaseWebViewModel(
    networkStateManager: NetworkStateManager,
    state: SavedStateHandle,
) : BaseViewModel(state) {

    override val connectionManager by lazy {
        ConnectionManager(
            networkStateManager,
            this
        )
    }

    val firstWebViewData: StateFlow<LoadState<MainWebViewData?>> by lazy { _firstWebViewData.asStateFlow() }

    val currentWebViewData: StateFlow<LoadState<MainWebViewData?>> by lazy { _currentWebViewData.asStateFlow() }

    val currentWebViewProgress: StateFlow<Int?> by lazy { _currentWebViewProgress.asStateFlow() }

    val currentUrl: StateFlow<Uri?> by lazy {
        currentWebViewData
            .map { it.data?.url }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

    val currentData: StateFlow<String?> by lazy {
        currentWebViewData
            .map { it.data?.data }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

    /**
     * Первые данные в WebView с состоянием загрузки/ошибки - после очередного вызова loadUrl/loadData
     */
    private val _firstWebViewData = MutableStateFlow<LoadState<MainWebViewData?>>(LoadState.initial())

    /**
     * Текущие данные в [WebView] с состоянием загрузки/ошибки
     */
    private val _currentWebViewData = MutableStateFlow<LoadState<MainWebViewData?>>(LoadState.initial())

    private val _currentWebViewProgress = MutableStateFlow<Int?>(null)

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
        _firstWebViewData.value = LoadState.initial()
        _currentWebViewData.value = LoadState.initial()
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
        val thisData: LoadState<MainWebViewData?> = resource.copyOf(
            fromWebViewData(resource.data, title)
        )

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
                title: String? = null,
            ): MainWebViewData? =
                data?.let {
                    MainWebViewData(it.url, it.data, title, it.response, it.responseData)
                }
        }
    }
}