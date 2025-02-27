package net.maxsmr.core.android.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * @return холодный [Flow] с периодическими эмитами
 */
fun tickerFlow(
    period: Duration,
    initialDelay: Duration = Duration.ZERO
) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

/**
 * @param direction направление отсчёта
 * @param initialDelay изначальная задержка
 * @param period интервал обновления таймера.
 * @return холодный [Flow] со значением от начала отсчёта в [Duration]
 * или `null`, когда таймер истек.
 */
fun timerFlow(
    duration: Duration,
    period: Duration,
    initialDelay: Duration = Duration.ZERO,
    direction: CountDirection = CountDirection.Up,
): Flow<Duration?> = flow {

    require(period.isPositive()) { "period value must be positive" }

    initialDelay.takeIf {
        it.isPositive()
    }?.let {
        delay(it)
    }

    val ticksCount = (duration / period).toInt()
    val remainderDuration = duration - period * ticksCount

    repeat(ticksCount) { tick ->
        val value = when (direction) {
            CountDirection.Down -> {
                duration - period * tick
            }

            CountDirection.Up -> {
                period * tick
            }
        }
        emit(value)
        delay(period)
    }

    if (remainderDuration != Duration.ZERO) {
        delay(remainderDuration)
    }

    emit(null)
}

enum class CountDirection {
    Up,
    Down
}