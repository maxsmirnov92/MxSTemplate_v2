package net.maxsmr.feature.compose_sample.ui.presentation.profile

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(state: SavedStateHandle): BaseViewModel(state)