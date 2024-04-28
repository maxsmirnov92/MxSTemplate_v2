package net.maxsmr.core.ui.views

import android.widget.AutoCompleteTextView
import net.maxsmr.core.ui.adapters.SuggestAdapter

fun AutoCompleteTextView.applySuggestions(data: List<String>) {
    (adapter as? SuggestAdapter)?.let { adapter ->
        adapter.setData(data)
        val isShowing = isPopupShowing
        if (data.isNotEmpty()) {
            if (!isShowing) {
                showDropDown()
            }
        } else if (isShowing) {
            dismissDropDown()
        }
    }
}