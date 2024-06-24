package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.feature.rate.fragment.BaseFeedbackViewModel
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.ui.fragment.DownloadableWebViewFragmentArgs
import net.maxsmr.mxstemplate.ui.fragment.FeedbackFragmentArgs

class FeedbackViewModel(state: SavedStateHandle): BaseFeedbackViewModel(state) {

    override val emailAddress: String = if (state.contains("emailAddress")) {
        FeedbackFragmentArgs.fromSavedStateHandle(state).emailAddress
    } else {
        EMPTY_STRING
    }
}