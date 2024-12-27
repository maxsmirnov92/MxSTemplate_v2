package net.maxsmr.core.ui.components

import android.content.Context
import net.maxsmr.core.android.base.BaseViewModel

interface IComponentDelegate<T> {

    val host: T

    val context: Context

    val viewModel: BaseViewModel

    fun onCreated() {}

    fun onResumed() {}

    fun onDestroyed() {}
}