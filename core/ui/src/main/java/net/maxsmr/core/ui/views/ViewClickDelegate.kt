package net.maxsmr.core.ui.views

import android.os.Handler
import android.os.Looper
import android.view.View
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

class ViewClickDelegate(
    private val view: View,
    private val targetClickCount: Int,
    private val interval: Long
) {

    init {
        require(targetClickCount >= 1) { "Incorrect targetClickCount: $targetClickCount" }
        require(interval >= 1) { "Incorrect interval: $interval" }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val callback = Runnable {
        currentClickCount = 0
    }

    private var currentClickCount: Int = 0

    fun setOnClickListener(action: () -> Unit) {
        handler.removeCallbacks(callback)
        currentClickCount = 0
        view.setOnClickListener {
            handler.removeCallbacks(callback)
            currentClickCount++
            if (currentClickCount >= targetClickCount) {
                currentClickCount = 0
                action()
            } else {
                handler.postDelayed(callback, interval)
            }
        }
    }
}