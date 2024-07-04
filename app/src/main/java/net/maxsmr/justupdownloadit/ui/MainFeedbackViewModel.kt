package net.maxsmr.justupdownloadit.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.core.android.screen.extractScreenParams
import net.maxsmr.feature.rate.fragment.BaseFeedbackViewModel
import net.maxsmr.justupdownloadit.ui.fragment.params.FeedbackScreenParams
import javax.inject.Inject

@HiltViewModel
class MainFeedbackViewModel @Inject constructor(
    state: SavedStateHandle,
    @ApplicationContext context: Context
): BaseFeedbackViewModel(state, context) {

    override val emailAddress: String by lazy {
        params.emailAddress
    }

    val params = state.extractScreenParams<FeedbackScreenParams>()
}