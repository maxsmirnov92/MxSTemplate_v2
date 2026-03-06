package net.maxsmr.core.ui.field

import androidx.lifecycle.viewModelScope
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.commonutils.flow.field.Field.Builder
import net.maxsmr.core.android.base.BaseViewModel

/**
 * Создание поля типа [T].
 * Указание лямбды emptyIf обязательно
 */
fun <T> BaseViewModel.createField(
    initialValue: T,
    key: String? = null,
    transformGet: ((T) -> T)? = null,
    transformSet: ((T) -> T)? = null,
    configure: Builder<T>.() -> Unit = {},
) = object : Builder<T>(initialValue, viewModelScope) {
    override fun transformGet(value: T): T {
        return transformGet?.invoke(value) ?: super.transformGet(value)
    }

    override fun transformSet(value: T): T {
        return transformSet?.invoke(value) ?: super.transformSet(value)
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
    transformGet: ((T) -> T)? = null,
    transformSet: ((T) -> T)? = null,
    configure: Builder<T>.() -> Unit = {},
) = createField(
    initialValue,
    key,
    transformGet,
    transformSet
) {
    emptyIf { false }
    configure()
}

fun BaseViewModel.createTextField(
    initialValue: String = "",
    key: String? = null,
    withTrim: Boolean = true,
    maxLength: Int = Int.MAX_VALUE,
    configure: Builder<String>.() -> Unit = {},
) = createTextField(
    initialValue = initialValue,
    key = key,
    transformGet = {
        if (withTrim) {
            it.trim()
        } else {
            it
        }
    },
    transformSet = {
        it.take(maxLength)
    }
) {
    configure()
}

private fun BaseViewModel.createTextField(
    initialValue: String = "",
    key: String? = null,
    transformGet: ((String) -> String)? = null,
    transformSet: ((String) -> String)? = null,
    configure: Field.Builder<String>.() -> Unit = {},
) = createField(
    initialValue,
    key,
    transformGet,
    transformSet
) {
    emptyIf { it.isEmpty() }
    configure()
}