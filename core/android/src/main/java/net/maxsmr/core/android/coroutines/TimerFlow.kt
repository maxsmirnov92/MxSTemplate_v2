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
 * @param period Интервал обновления таймера.
 * @return холодный [Flow] со значением от начала отсчёта в единицах [targetUnit]
 * или `null`, когда таймер истек.
 */
fun timerFlow(
    targetUnit: TimeUnit,
    period: Duration,
    direction: CountDirection,
    initialDelay: Duration = Duration.ZERO,
): Flow<Long?> = flow {

    period.takeIf { it.isPositive() } ?: run {
        emit(null)
        return@flow
    }

    val periodUnits = period.toTimeUnit(targetUnit)

    suspend fun Long.emit() {
        emit(this)
        delay(period)
    }

    initialDelay.takeIf {
        it.isPositive()
    }?.toTimeUnit(targetUnit)?.emit()

    when (direction) {
        is CountDirection.Down -> {
            val durationUnits = direction.duration.toTimeUnit(targetUnit)
            val endUnits = direction.end.takeIf { it.isPositive() }?.toTimeUnit(targetUnit) ?: 0L
            for (remaining in durationUnits downTo endUnits step periodUnits) {
                remaining.emit()
            }
        }

        is CountDirection.Up -> {
            val durationUnits = direction.duration.toTimeUnit(targetUnit)
            val startUnits =
                direction.start.takeIf { it.isPositive() }?.toTimeUnit(targetUnit) ?: 0L
            for (elapsed in startUnits..durationUnits step periodUnits) {
                elapsed.emit()
            }
        }

        else -> {
            var current = 0L
            while (true) {
                current.emit()
                current += periodUnits
            }
        }
    }

    emit(null)
}


sealed interface CountDirection {

    data class Up(val duration: Duration, val start: Duration = Duration.ZERO) : CountDirection
    data class Down(val duration: Duration, val end: Duration = Duration.ZERO) : CountDirection
    data object Indefinite : CountDirection
}