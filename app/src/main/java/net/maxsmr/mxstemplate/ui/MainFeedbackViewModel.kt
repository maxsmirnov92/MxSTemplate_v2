package net.maxsmr.mxstemplate.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.feature.rate.fragment.BaseFeedbackViewModel
import net.maxsmr.mxstemplate.ui.fragment.MainFeedbackFragmentArgs
import javax.inject.Inject

@HiltViewModel
class MainFeedbackViewModel @Inject constructor(
    state: SavedStateHandle,
    @ApplicationContext context: Context
): BaseFeedbackViewModel(state, context) {

    override val emailAddress: String = if (state.contains("emailAddress")) {
        MainFeedbackFragmentArgs.fromSavedStateHandle(state).emailAddress
    } else {
        EMPTY_STRING
    }
}