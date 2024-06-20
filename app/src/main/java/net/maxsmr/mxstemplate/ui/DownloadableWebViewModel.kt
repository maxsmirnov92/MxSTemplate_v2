package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.ui.fragment.DownloadableWebViewFragmentArgs

class DownloadableWebViewModel(state: SavedStateHandle) : BaseDownloadableWebViewModel(state) {

    override var customizer: WebViewCustomizer = if (state.contains("customizer")) {
        DownloadableWebViewFragmentArgs.fromSavedStateHandle(state).customizer
    } else {
        WebViewCustomizer.Builder().build()
    }
}