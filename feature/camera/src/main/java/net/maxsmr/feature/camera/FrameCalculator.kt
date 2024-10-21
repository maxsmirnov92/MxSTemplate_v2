package net.maxsmr.feature.camera

import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import net.maxsmr.commonutils.collection.avg
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.io.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * @param notifyHandler [Handler] on which [onFrameStatsUpdated] should be called; null -> main thread
 * @param onFrameStatsUpdated last FrameStats + frames since last notify
 */
class FrameCalculator(
    notifyHandler: Handler? = null,
    private val onFrameStatsUpdated: (FrameStats, Long) -> Unit,
) {

    constructor(notifyLooper: Looper?, onFrameStatsUpdated: (FrameStats, Long) -> Unit) : this(
        notifyLooper?.let { Handler(notifyLooper) },
        onFrameStatsUpdated
    )

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(FrameCalculator::class.java)

    private val notifyHandler: Handler = notifyHandler ?: Handler(Looper.getMainLooper())

    private var calcExecutor: ExecutorService? = null

    private val frameTimesDuringInterval = mutableListOf<Long>()

    val isStarted: Boolean
        @Synchronized
        get() = startTime > 0

    var calcDiff: Long = CALCULATE_DIFF_DEFAULT
        set(value) {
            require(value > 0) { "Incorrect calc diff: $value" }
//            if (value > notifyInterval) {
//                field = CALCULATE_DIFF_DEFAULT;
//            }
            field = value
        }

    var resetDiff: Long = RESET_DIFF_DEFAULT
        set(value) {
            require(value >= 0) { "Incorrect reset diff: $value" }
            field = value
        }

    var notifyInterval = NOTIFY_INTERVAL_DEFAULT
        set(value) {
            require(value >= 0) { "Incorrect notify interval: $value" }
//            if (value != 0L && value < calcDiff) {
//                field = NOTIFY_INTERVAL_DEFAULT;
//            }
            field = value
        }

    var lastStats = FrameStats()
        private set

    var startTime: Long = 0
        private set

    var startIntervalTime: Long = 0
        private set

    var lastNotifyTime: Long = 0
        private set

    var lastFrameTime: Long = 0
        private set

    var intervalFrames = 0
        private set

    var lastFps = 0.0
        private set

    private var lastAverageFrameTimeDuringInterval = 0.0

    private var lastNotifyFramesCount: Long = 0

    private var totalFrames: Long = 0
    private var totalFpsSum: Double = 0.0
    private var totalFpsCount = 0
    private var totalFrameTimeSum = 0.0
    private var totalFrameTimesCount = 0

    @Synchronized
    fun onStart() {
        logger.d("onStart")
        resetCounters()
        lastStats = FrameStats()
        startTime = System.currentTimeMillis()
        startExec()
    }

    @Synchronized
    fun onStop() {
        logger.d("onStop")
        stopExec()
        startTime = 0
    }

    @JvmOverloads
    fun onFrame(timestamp: Long = System.currentTimeMillis()): Long {
        if (!isStarted) {
            onStart()
        }
        calcExecutor?.execute(FrameCalcRunnable(timestamp))
        return timestamp
    }

    fun getAverageFrameTime(): Double {
        return if (totalFrameTimesCount > 0) totalFrameTimeSum / totalFrameTimesCount else 0.0
    }

    fun getAverageFpsMethod1(): Double {
        return if (totalFpsCount > 0) totalFpsSum / totalFpsCount else lastFps
    }

    fun getAverageFpsMethod2(): Double {
        val currentTime = System.currentTimeMillis()
        val measureTime = if (startTime > 0) (currentTime - startTime) / 1000 else 0
        return if (measureTime > 0) totalFrames.toDouble() / measureTime else lastFps
    }

    @Synchronized
    fun resetCounters() {
        lastFrameTime = 0
        lastAverageFrameTimeDuringInterval = 0.0
        frameTimesDuringInterval.clear()
        totalFrameTimeSum = 0.0
        totalFrameTimesCount = 0

        startIntervalTime = 0
        intervalFrames = 0
        lastFps = 0.0
        totalFpsSum = 0.0
        totalFpsCount = 0
        lastNotifyFramesCount = 0
        totalFrames = 0
    }

    @Synchronized
    private fun isExecRunning(): Boolean {
        return calcExecutor?.isShutdown == false
    }

    @Synchronized
    private fun startExec() {
        if (isExecRunning()) return
        calcExecutor = Executors.newSingleThreadExecutor()
    }

    @Synchronized
    private fun stopExec() {
        if (isExecRunning()) {
            calcExecutor?.let {
                it.shutdown()
                calcExecutor = null
            }
        }
    }

    private inner class FrameCalcRunnable(private val eventTimestamp: Long) : Runnable {

        init {
            require(eventTimestamp > 0) { "Incorrect event time: $eventTimestamp" }
            if (startIntervalTime == 0L) {
                startIntervalTime = eventTimestamp
            }
        }

        override fun run() {
            if (eventTimestamp < startIntervalTime) {
                logger.e("Event time ($eventTimestamp ms) < start interval time ($startIntervalTime ms)")
                return
            }

            if (resetDiff > 0 && (eventTimestamp - startIntervalTime >= resetDiff)) {
                val startInterval = startIntervalTime
                resetCounters()
                startIntervalTime = startInterval
            }

            if (lastFrameTime != 0L && lastFrameTime < eventTimestamp) {
                frameTimesDuringInterval.add(eventTimestamp - lastFrameTime)
            }
            lastFrameTime = eventTimestamp

            intervalFrames++

            val diff = eventTimestamp - startIntervalTime
            if (diff >= calcDiff) {
                lastAverageFrameTimeDuringInterval = if (frameTimesDuringInterval.isNotEmpty()) {
                    frameTimesDuringInterval.avg()
                } else {
                    0.0
                }
                frameTimesDuringInterval.clear()
                totalFrameTimeSum += lastAverageFrameTimeDuringInterval
                totalFrameTimesCount++

                totalFrames += intervalFrames
                lastFps = intervalFrames * TimeUnit.SECONDS.toMillis(1) / diff.toDouble()
                totalFpsSum += lastFps
                totalFpsCount++
                intervalFrames = 0

                startIntervalTime = 0

                lastStats = FrameStats(
                    startTime,
                    lastFps,
                    lastAverageFrameTimeDuringInterval.roundToLong(),
                    getAverageFpsMethod1(),
                    getAverageFrameTime().roundToLong()
                )
            }

            if (notifyInterval == 0L || (lastNotifyTime <= 0 || (eventTimestamp - lastNotifyTime) >= notifyInterval)) {
                val framesSinceLastNotify: Long = totalFrames - lastNotifyFramesCount
                notifyHandler.post {
                    onFrameStatsUpdated(lastStats, framesSinceLastNotify)
                }
                lastNotifyTime = eventTimestamp
            }
            lastNotifyFramesCount = totalFrames
        }
    }

    data class FrameStats(
        val startTime: Long,
        val lastFps: Double,
        val lastAverageFrameTime: Long,
        val overallAverageFps: Double,
        val overallAverageFrameTime: Long,
    ) : Parcelable, Serializable {

        constructor() : this(
            0L, 0.0, 0L, 0.0, 0L
        )

        constructor(`in`: Parcel) : this(
            startTime = `in`.readLong(),
            lastFps = `in`.readDouble(),
            lastAverageFrameTime = `in`.readLong(),
            overallAverageFps = `in`.readDouble(),
            overallAverageFrameTime = `in`.readLong(),
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FrameStats) return false

            if (startTime != other.startTime) return false
            if (lastFps != other.lastFps) return false
            if (lastAverageFrameTime != other.lastAverageFrameTime) return false
            if (overallAverageFps != other.overallAverageFps) return false
            if (overallAverageFrameTime != other.overallAverageFrameTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = startTime.hashCode()
            result = 31 * result + lastFps.hashCode()
            result = 31 * result + lastAverageFrameTime.hashCode()
            result = 31 * result + overallAverageFps.hashCode()
            result = 31 * result + overallAverageFrameTime.hashCode()
            return result
        }

        override fun toString(): String {
            return "FrameStats{" +
                    "lastFps=" + lastFps +
                    ", lastAverageFrameTime=" + lastAverageFrameTime +
                    ", overallAverageFps=" + overallAverageFps +
                    ", overallAverageFrameTime=" + overallAverageFrameTime +
                    '}'
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(parcel: Parcel, i: Int) {
            parcel.writeLong(startTime)
            parcel.writeDouble(lastFps)
            parcel.writeLong(lastAverageFrameTime)
            parcel.writeDouble(overallAverageFps)
            parcel.writeLong(overallAverageFrameTime)
        }

        companion object {

            @JvmField
            val CREATOR: Parcelable.Creator<FrameStats> = object : Parcelable.Creator<FrameStats> {
                override fun createFromParcel(`in`: Parcel): FrameStats {
                    return FrameStats(`in`)
                }

                override fun newArray(size: Int): Array<FrameStats?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {

        @JvmStatic
        val CALCULATE_DIFF_DEFAULT: Long = TimeUnit.SECONDS.toMillis(1)

        @JvmStatic
        val RESET_DIFF_DEFAULT: Long = TimeUnit.MINUTES.toMillis(1)

        @JvmStatic
        val NOTIFY_INTERVAL_DEFAULT: Long = CALCULATE_DIFF_DEFAULT
    }
}