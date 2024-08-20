package net.maxsmr.core.ui.views

import android.widget.AutoCompleteTextView
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.ui.adapters.SuggestAdapter

fun AutoCompleteTextView.applySuggestions(state: LoadState<List<String>>) {
    (adapter as? SuggestAdapter)?.let { adapter ->
        adapter.setData(state)
        toggleDropDown()
    }
}

fun AutoCompleteTextView.toggleDropDown() {
    (adapter as? SuggestAdapter)?.let { adapter ->
        val isShowing = isPopupShowing
        if (!adapter.isEmpty) {
            if (!isShowing) {
                showDropDown()
            }
        } else if (isShowing) {
            dismissDropDown()
        }
    }
}