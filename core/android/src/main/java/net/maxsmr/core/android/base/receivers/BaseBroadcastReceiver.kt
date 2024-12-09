package net.maxsmr.core.android.base.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

abstract class BaseBroadcastReceiver: BroadcastReceiver() {

    protected val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>(javaClass)

    abstract val intentActions: Set<String>
    
    abstract fun doAction(context: Context, intent: Intent)
    
    open fun isAcceptableForAction(context: Context, intent: Intent) = intent.action in intentActions

    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {
        if (isAcceptableForAction(context, intent)) {
            logger.d("onReceive, intent: $intent")
            doAction(context, intent)
        }
    }
}