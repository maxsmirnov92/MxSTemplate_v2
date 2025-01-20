package net.maxsmr.feature.compose_sample.ui.presentation.home

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(state: SavedStateHandle): BaseViewModel(state)