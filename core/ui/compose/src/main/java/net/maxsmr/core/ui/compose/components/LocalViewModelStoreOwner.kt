package net.maxsmr.core.ui.compose.components

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class LocalViewModelStoreOwner: ViewModelStoreOwner {

    private val _viewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    fun clear() {
        _viewModelStore.clear()
    }
}