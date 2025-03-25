package net.maxsmr.core.ui.field

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.core.android.base.BaseViewModel

fun <T> BaseViewModel.createField(
    initialValue: T,
    key: String? = null,
    valueGetter: ((T) -> T)? = null,
    valueSetter: ((MutableSharedFlow<T>) -> Unit)? = null,
    configure: Field.Builder<T>.() -> Unit = {},
) = object : Field.Builder<T>(initialValue, viewModelScope) {

    override fun valueGetter(fieldValue: StateFlow<T>): () -> T {
        return valueGetter?.let {
            { valueGetter(fieldValue.value) }
        } ?: super.valueGetter(fieldValue)
    }

    override fun valueSetter(fieldValue: MutableSharedFlow<T>): (T) -> Unit {
        return valueSetter?.let {
            { valueSetter(fieldValue) }
        } ?: super.valueSetter(fieldValue)
    }
}.apply {
    if (key != null) {
        persist(state, key)
    }
    configure()
}.build()


fun <T> BaseViewModel.createNonEmptyField(
    initialValue: T,
    key: String? = null,
    valueGetter: ((T) -> T)? = null,
    valueSetter: ((MutableSharedFlow<T>) -> Unit)? = null,
    configure: Field.Builder<T>.() -> Unit = {},
) = createField(
    initialValue,
    key,
    valueGetter,
    valueSetter
) {
    emptyIf { false }
    configure()
}

fun BaseViewModel.createTextField(
    initialValue: String = "",
    key: String? = null,
    valueGetter: ((String) -> String)? = null,
    valueSetter: ((MutableSharedFlow<String>) -> Unit)? = null,
    configure: Field.Builder<String>.() -> Unit = {},
) = createField(
    initialValue,
    key,
    valueGetter,
    valueSetter
) {
    emptyIf { it.isEmpty() }
    configure()
}