package net.maxsmr.justupdownloadit.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.feature.rate.fragment.BaseFeedbackViewModel
import net.maxsmr.justupdownloadit.ui.fragment.FeedbackFragmentArgs

class FeedbackViewModel(state: SavedStateHandle): BaseFeedbackViewModel(state) {

    override val emailAddress: String = if (state.contains("emailAddress")) {
        FeedbackFragmentArgs.fromSavedStateHandle(state).emailAddress
    } else {
        EMPTY_STRING
    }
}