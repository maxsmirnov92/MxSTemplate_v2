package net.maxsmr.mxstemplate.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.screen.extractScreenParamsOrNull
import net.maxsmr.feature.about.AboutViewModel
import net.maxsmr.mxstemplate.ui.fragment.params.AboutScreenParams
import javax.inject.Inject

@HiltViewModel
class MainAboutViewModel @Inject constructor(state: SavedStateHandle) : AboutViewModel(state) {

    val isForRate = state.extractScreenParamsOrNull<AboutScreenParams>()?.isForRate ?: false
}