package net.maxsmr.core.android.base.delegates

import androidx.lifecycle.viewModelScope
import net.maxsmr.commonutils.PersistableLiveData
import net.maxsmr.commonutils.PersistableLiveDataInitial
import net.maxsmr.commonutils.PersistableStateFlow
import net.maxsmr.commonutils.PersistableValue
import net.maxsmr.commonutils.persistableLiveData
import net.maxsmr.commonutils.persistableLiveDataInitial
import net.maxsmr.commonutils.persistableStateFlow
import net.maxsmr.commonutils.persistableValue
import net.maxsmr.commonutils.persistableValueInitial
import net.maxsmr.core.android.base.BaseViewModel

fun <T> BaseViewModel.persistableValue(
    onSetValue: ((T?) -> Unit)? = null,
): PersistableValue<T> = state.persistableValue(onSetValue)

fun <T> BaseViewModel.persistableValueInitial(
    initialValue: T,
    onSetValue: ((T) -> Unit)? = null,
) = state.persistableValueInitial(initialValue, onSetValue)

fun <T> BaseViewModel.persistableLiveData(): PersistableLiveData<T> =
    state.persistableLiveData()

fun <T> BaseViewModel.persistableLiveDataInitial(
    initialValue: T,
): PersistableLiveDataInitial<T> = state.persistableLiveDataInitial(initialValue)

fun <T> BaseViewModel.persistableStateFlow(
    initialValue: T,
): PersistableStateFlow<T> = state.persistableStateFlow(viewModelScope, initialValue)