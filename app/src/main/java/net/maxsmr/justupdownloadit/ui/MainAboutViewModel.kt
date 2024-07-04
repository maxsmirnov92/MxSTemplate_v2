package net.maxsmr.justupdownloadit.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.core.android.screen.extractScreenParamsOrNull
import net.maxsmr.feature.about.AboutViewModel
import net.maxsmr.justupdownloadit.ui.fragment.params.AboutScreenParams
import javax.inject.Inject

@HiltViewModel
class MainAboutViewModel @Inject constructor(
    state: SavedStateHandle,
    @ApplicationContext context: Context,
) : AboutViewModel(state, context) {

    val isForRate = state.extractScreenParamsOrNull<AboutScreenParams>()?.isForRate ?: false
}