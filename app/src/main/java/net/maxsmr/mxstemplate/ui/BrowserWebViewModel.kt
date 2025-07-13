package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.android.screen.extractScreenParams
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.ui.fragment.params.WebViewScreenParams
import javax.inject.Inject

@HiltViewModel
class BrowserWebViewModel @Inject constructor(
    networkStateManager: NetworkStateManager,
    state: SavedStateHandle,
) : BaseDownloadableWebViewModel(networkStateManager, state) {

    override var customizer: WebViewCustomizer =
        state.extractScreenParams<WebViewScreenParams>().customizer
}