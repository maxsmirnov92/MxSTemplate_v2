package net.maxsmr.core.android.base.delegates

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.core.android.base.BaseViewModel
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


fun <T: Any> T.getPersistableKey(kProperty: KProperty<*>): String =
        "${this::class.simpleName}.${kProperty.name}"

fun <T> BaseViewModel.persistableLiveData(): PersistableLiveData<T> = PersistableLiveData(state)

fun <T> BaseViewModel.persistableLiveDataInitial(initialValue: T): PersistableLiveDataInitial<T> =
        PersistableLiveDataInitial(state, initialValue)

fun <T> BaseViewModel.persistableValue(onSetValue: ((T?) -> Unit)? = null): PersistableValue<T> =
        PersistableValue(state, onSetValue)

fun <T> BaseViewModel.persistableValueInitial(
        initialValue: T,
        onSetValue: ((T) -> Unit)? = null,
): PersistableValueInitial<T> = PersistableValueInitial(state, initialValue, onSetValue)


/**
 * Используйте, если нужна LiveData, переживающая смерть процесса приложения
 */
class PersistableLiveData<T>(
        private val state: SavedStateHandle
) : ReadOnlyProperty<Any, MutableLiveData<T>> {

    override fun getValue(thisRef: Any, property: KProperty<*>): MutableLiveData<T> {
        return state.getLiveData(thisRef.getPersistableKey(property))
    }
}


/**
 * Используйте, если нужна LiveData, переживающая смерть процесса приложения, с начальным значением.
 * Начальное значение гарантирует, что LiveData всегда содержит какое-либо значение и field.value
 * не вернет null.
 */
class PersistableLiveDataInitial<T>(
        private val state: SavedStateHandle,
        private val initialValue: T,
) : ReadOnlyProperty<Any, MutableLiveData<T>> {

    override fun getValue(thisRef: Any, property: KProperty<*>): MutableLiveData<T> {
        return state.getLiveData(thisRef.getPersistableKey(property), initialValue)
    }
}


/**
 * Используйте, если нужно поле, переживающее смерть процесса приложения
 *
 * @param onSetValue доп. действие при смене значения
 */
class PersistableValue<T>(
        private val state: SavedStateHandle,
        private val onSetValue: ((T?) -> Unit)? = null,
) : ReadWriteProperty<Any, T?> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return state[thisRef.getPersistableKey(property)]
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        state[thisRef.getPersistableKey(property)] = value
        onSetValue?.invoke(value)
    }
}


/**
 * Используйте, если нужно поле, переживающее смерть процесса приложения, с начальным значением.
 * Начальное значение гарантирует, что [getValue] не вернет null.
 *
 * @param initial начальное значение
 * @param onSetValue доп. действие при смене значения
 */
class PersistableValueInitial<T>(
        private val state: SavedStateHandle,
        private val initial: T,
        private val onSetValue: ((T) -> Unit)? = null,
) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return state[thisRef.getPersistableKey(property)] ?: initial
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        state[thisRef.getPersistableKey(property)] = value
        onSetValue?.invoke(value)
    }
}