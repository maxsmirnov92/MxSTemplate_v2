package net.maxsmr.mxstemplate.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.ui.fragment.BrowserWebViewFragmentArgs
import javax.inject.Inject

@HiltViewModel
class BrowserWebViewModel @Inject constructor(
    state: SavedStateHandle,
    @ApplicationContext context: Context
) : BaseDownloadableWebViewModel(state, context) {

    override var customizer: WebViewCustomizer = if (state.contains(ARG_WEB_CUSTOMIZER)) {
        BrowserWebViewFragmentArgs.fromSavedStateHandle(state).customizer
    } else {
        WebViewCustomizer.Builder().build()
    }

    companion object {

        const val ARG_WEB_CUSTOMIZER = "customizer"
    }
}