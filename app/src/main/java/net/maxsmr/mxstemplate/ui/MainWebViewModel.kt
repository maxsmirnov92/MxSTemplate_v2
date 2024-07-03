package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.ui.BrowserWebViewModel.Companion.ARG_WEB_CUSTOMIZER
import net.maxsmr.mxstemplate.ui.fragment.MainWebViewFragmentArgs

class MainWebViewModel(state: SavedStateHandle) : BaseDownloadableWebViewModel(state) {

    override var customizer: WebViewCustomizer = if (state.contains(ARG_WEB_CUSTOMIZER)) {
        MainWebViewFragmentArgs.fromSavedStateHandle(state).customizer
    } else {
        WebViewCustomizer.Builder().build()
    }
}