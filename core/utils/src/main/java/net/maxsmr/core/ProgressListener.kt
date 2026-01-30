package net.maxsmr.core

import androidx.annotation.CallSuper
import net.maxsmr.commonutils.stream.StreamCancellationException
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

abstract class ProgressListener {

    open val notifyInterval: Long = 0L

    private var lastNotifyTime: Long = 0

    /**
     * @return false для прекращения чтения из источника
     */
    abstract fun onProcessing(state: ProgressStateInfo): Boolean

    @Throws(StreamCancellationException::class)
    fun notifyWithCheckOrThrow(
        currentBytes: Long,
        totalBytes: Long,
        startTime: Long,
    ) {
        val interval = notifyInterval

        val currentTime = System.currentTimeMillis()

        if (interval >= 0 && (interval == 0L || lastNotifyTime == 0L || currentTime - lastNotifyTime >= interval)) {
            if (!notify(currentBytes, totalBytes, startTime, currentTime)) {
                throw StreamCancellationException()
            }
        }
    }

    @CallSuper
    fun notify(
        currentBytes: Long,
        totalBytes: Long,
        startTime: Long,
        currentTime: Long,
    ): Boolean {
        val elapsedTimeMillis = currentTime - startTime
        val elapsedTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime)

        val speedMillis = if (elapsedTimeMillis > 0) {
            currentBytes.toDouble() / elapsedTimeMillis
        } else {
            0.0
        }
        val estimatedTimeSeconds = if (totalBytes > currentBytes && speedMillis > 0) {
            TimeUnit.MILLISECONDS.toSeconds(((totalBytes - currentBytes) / speedMillis).toLong())
        } else {
            0
        }

        if (!onProcessing(
                    ProgressStateInfo(
                        currentBytes,
                        totalBytes,
                        speedMillis * 1000,
                        elapsedTimeSeconds,
                        estimatedTimeSeconds
                    )
                )
        ) {
            return false
        }

        lastNotifyTime = System.currentTimeMillis()
        return true
    }

    /**
     * @param totalBytes неотрицательный общий размер
     * @param speed скорость в байт/с
     * @param elapsedTime пройдённое время в секундах от начала загрузки
     * @param estimatedTime оцениваемое время в секундах до конца загрузки
     */
    data class ProgressStateInfo(
        val currentBytes: Long,
        val totalBytes: Long,
        val speed: Double,
        val elapsedTime: Long,
        val estimatedTime: Long,
    ) : Serializable {

        val progressRatio: Float? = when {

            totalBytes > 0 -> currentBytes.toFloat() / totalBytes

            totalBytes < 0 -> null

            else -> 0f
        }

        val progress: Float? = progressRatio?.takeIf { it >= 0 }?.let { it * 100 }

        val progressRounded: Int? = progress?.roundToInt()

        val done: Boolean = if (totalBytes >= 0) {
            currentBytes >= totalBytes
        } else {
            false
        }

        override fun toString(): String {
            return "ProgressStateInfo(currentBytes=$currentBytes, " +
                    "totalBytes=$totalBytes, " +
                    "speed=$speed, " +
                    "elapsedTime=$elapsedTime, " +
                    "estimatedTime=$estimatedTime, " +
                    "progressRatio=$progressRatio)"
        }
    }
}