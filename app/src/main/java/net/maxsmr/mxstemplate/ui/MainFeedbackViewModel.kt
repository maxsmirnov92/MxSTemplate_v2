package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.screen.extractScreenParams
import net.maxsmr.feature.rate.fragment.BaseFeedbackViewModel
import net.maxsmr.mxstemplate.ui.fragment.params.FeedbackScreenParams
import javax.inject.Inject

@HiltViewModel
class MainFeedbackViewModel @Inject constructor(state: SavedStateHandle): BaseFeedbackViewModel(state) {

    override val emailAddress: String by lazy {
        params.emailAddress
    }

    val params = state.extractScreenParams<FeedbackScreenParams>()
}