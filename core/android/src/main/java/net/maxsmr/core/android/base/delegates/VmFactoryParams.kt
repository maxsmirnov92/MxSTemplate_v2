package net.maxsmr.core.android.base.delegates

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Базовая реализация [AbstractSavedStateViewModelFactory]
 */
fun <T> AbstractSavedStateViewModelFactory(
    savedStateRegistryOwner: SavedStateRegistryOwner,
    create: (handle: SavedStateHandle) -> T,
) = object : AbstractSavedStateViewModelFactory(savedStateRegistryOwner, null) {
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle) =
        create.invoke(handle) as T
}