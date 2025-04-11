package net.maxsmr.mxstemplate.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.feature.about.AboutViewModel
import net.maxsmr.mxstemplate.ui.fragment.MainAboutFragmentArgs
import javax.inject.Inject

@HiltViewModel
class MainAboutViewModel @Inject constructor(
    state: SavedStateHandle,
    @ApplicationContext context: Context
): AboutViewModel(state, context) {

    // этот филд для из-за того, что by navArgs упадёт при навигации без аргументов
    val isForRate = if (state.contains("isForRate")) {
        MainAboutFragmentArgs.fromSavedStateHandle(state).isForRate
    } else {
        false
    }
}