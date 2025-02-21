package net.maxsmr.core.android.coroutines

import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toDuration
import kotlin.time.toDurationUnit

fun Duration.toTimeUnit(targetUnit: TimeUnit): Long {
    return targetUnit.convert(this.inWholeNanoseconds, TimeUnit.NANOSECONDS)
}

fun timeUnitToDuration(value: Long, timeUnit: TimeUnit): Duration {
    return value.toDuration(timeUnit.toDurationUnit())
}