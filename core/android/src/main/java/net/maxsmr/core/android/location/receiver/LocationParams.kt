package net.maxsmr.core.android.location.receiver

import java.util.concurrent.TimeUnit

data class LocationParams @JvmOverloads constructor(
    val interval: Long = TimeUnit.SECONDS.toMillis(15),
    val minDistance: Long = 5,
    val priority: Priority = Priority.BALANCED
) {

    enum class Priority {
        HIGH,
        BALANCED,
        PASSIVE;

        companion object {

            fun resolve(value: Int): Priority? = entries.find { it.ordinal == value }
        }
    }
}