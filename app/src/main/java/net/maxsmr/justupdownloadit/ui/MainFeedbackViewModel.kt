package net.maxsmr.justupdownloadit.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.feature.rate.fragment.BaseFeedbackViewModel
import net.maxsmr.justupdownloadit.ui.fragment.MainFeedbackFragmentArgs

class MainFeedbackViewModel(state: SavedStateHandle): BaseFeedbackViewModel(state) {

    override val emailAddress: String = if (state.contains("emailAddress")) {
        MainFeedbackFragmentArgs.fromSavedStateHandle(state).emailAddress
    } else {
        EMPTY_STRING
    }
}