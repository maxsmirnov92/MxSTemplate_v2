package net.maxsmr.feature.compose_sample.ui.presentation.favourite

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.core.android.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class FavouriteViewModel @Inject constructor(state: SavedStateHandle) : BaseViewModel(state) {

    val field = Field.Builder("field1")
        .emptyIf { it.isEmpty() }
        .setRequired(TextMessage("empty error"))
        .hint(TextMessage("hint1"))
        .build()
}