package net.maxsmr.core.ui.views.edit

import android.text.Editable

import android.text.TextWatcher

fun interface TextChangeListener : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
}

