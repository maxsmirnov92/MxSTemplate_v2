package net.maxsmr.feature.demo.strategies

import android.content.Context
import android.widget.Toast
import net.maxsmr.feature.demo.R

class ToastDemoExpiredStrategy(
    private val context: Context,
    private val messageArg: String? = null
): IDemoExpiredStrategy {

    override fun doAction() {
        Toast.makeText(context, if (messageArg != null) {
            context.getString(R.string.demo_period_expired_message_format, messageArg)
        } else {
            context.getString(R.string.demo_period_expired_message)
        }, Toast.LENGTH_SHORT).show()
    }
}