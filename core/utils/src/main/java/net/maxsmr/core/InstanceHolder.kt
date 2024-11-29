package net.maxsmr.core

import java.util.concurrent.ConcurrentHashMap

open class InstanceHolder<Key, Value>(private val createFunc: (Key) -> Value) {

    private val cache = ConcurrentHashMap<Key, Value>()

    fun get(key: Key): Value {
        return cache.computeIfAbsent(key) { createFunc(key) }
    }

    fun remove(key: Key): Value? {
        return cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }
}