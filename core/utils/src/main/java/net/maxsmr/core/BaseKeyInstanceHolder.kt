package net.maxsmr.core

abstract class BaseKeyInstanceHolder<Key, Value>(
    createFunc: (Key) -> Value
): InstanceHolder<Key, Value>(createFunc) {

    abstract val key: Key

    fun get() = get(key)

    fun remove(): Value? = remove(key)
}