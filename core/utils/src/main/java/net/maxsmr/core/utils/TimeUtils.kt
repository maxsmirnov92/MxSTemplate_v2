package net.maxsmr.core.utils

fun hasTimePassed(timestamp: Long, interval: Long): Boolean {
    if (interval <= 0) return false
    val current = System.currentTimeMillis()
    return current - timestamp >= interval
}