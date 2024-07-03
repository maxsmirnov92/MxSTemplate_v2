package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.ui.fragment.BrowserWebViewFragmentArgs

class BrowserWebViewModel(state: SavedStateHandle) : BaseDownloadableWebViewModel(state) {

    override var customizer: WebViewCustomizer = if (state.contains(ARG_WEB_CUSTOMIZER)) {
        BrowserWebViewFragmentArgs.fromSavedStateHandle(state).customizer
    } else {
        WebViewCustomizer.Builder().build()
    }

    companion object {

        const val ARG_WEB_CUSTOMIZER = "customizer"
    }
}