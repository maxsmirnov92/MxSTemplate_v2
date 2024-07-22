package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.feature.about.AboutViewModel
import net.maxsmr.mxstemplate.ui.fragment.MainAboutFragmentArgs

class MainAboutViewModel(state: SavedStateHandle): AboutViewModel(state) {

    // этот филд для из-за того, что by navArgs упадёт при навигации без аргументов
    val isForRate = if (state.contains("isForRate")) {
        MainAboutFragmentArgs.fromSavedStateHandle(state).isForRate
    } else {
        false
    }
}