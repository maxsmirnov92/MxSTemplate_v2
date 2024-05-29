package net.maxsmr.feature.download.ui.webview

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.feature.webview.ui.BaseCustomizableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer

class DownloadableWebViewModel(state: SavedStateHandle): BaseCustomizableWebViewModel(state) {

    override var customizer: WebViewCustomizer = DownloadableWebViewFragmentArgs.fromSavedStateHandle(state).customizer
}