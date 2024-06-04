package net.maxsmr.core

import androidx.annotation.CallSuper
import java.io.InterruptedIOException
import java.io.Serializable
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

abstract class ProgressListener {

    open val notifyInterval: Long = 0L

    private var lastNotifyTime: Long = 0

    /**
     * @return false для прекращения чтения из источника
     */
    abstract fun onProcessing(state: ProgressStateInfo): Boolean

    @Throws(CancellationException::class)
    @CallSuper
    open fun notify(currentBytes: Long, totalBytes: Long, done: Boolean, startTime: Long) {

        val interval = notifyInterval
        val currentTime = System.currentTimeMillis()

        if (done || interval >= 0 && (interval == 0L || lastNotifyTime == 0L || currentTime - lastNotifyTime >= interval)) {
            
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
                            estimatedTimeSeconds,
                            done
                        )
                    )
            ) {
                throw CancellationException("Process interrupted")
            }
            lastNotifyTime = System.currentTimeMillis()
        }
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
        val done: Boolean,
    ) : Serializable {

        val progress: Float = if (totalBytes > 0) {
            (currentBytes * 100f) / totalBytes
        } else {
            0f
        }

        val progressRounded: Int = progress.roundToInt()

        override fun toString(): String {
            return "ProgressStateInfo(currentBytes=$currentBytes, " +
                    "totalBytes=$totalBytes, " +
                    "speed=$speed, " +
                    "elapsedTime=$elapsedTime, " +
                    "estimatedTime=$estimatedTime, " +
                    "progress=$progress)"
        }
    }
}