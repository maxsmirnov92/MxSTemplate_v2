package net.maxsmr.justupdownloadit.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.core.android.screen.extractScreenParams
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.justupdownloadit.ui.fragment.params.WebViewScreenParams
import javax.inject.Inject

@HiltViewModel
class MainWebViewModel @Inject constructor(
    state: SavedStateHandle,
    @ApplicationContext context: Context,
) : BaseDownloadableWebViewModel(state, context) {

    override var customizer: WebViewCustomizer =
        state.extractScreenParams<WebViewScreenParams>().customizer
}