package net.maxsmr.core.android.base.receivers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import net.maxsmr.commonutils.isAtLeastNougat
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.core.android.baseApplicationContext

abstract class BaseBootCompleteReceiver : BaseBroadcastReceiver() {

    /**
     * Последнее запомненное значение из [Settings.Global.BOOT_COUNT] или null,
     * если проверка от повторных срабатываний не требуется
     * (ACTION_BOOT_COMPLETED после ACTION_LOCKED_BOOT_COMPLETED)
     */
    abstract var lastBootCount: Int?

    private fun getCurrentBootCount(context: Context): Int? =
        if (isAtLeastNougat()) {
            getBootCount(context)
        } else {
            null
        }

    override val intentActions: Set<String> = mutableSetOf(
        Intent.ACTION_BOOT_COMPLETED,
        "android.intent.action.QUICKBOOT_POWERON"
    ).apply {
        if (isAtLeastNougat()) {
            add(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        }
    }

    override fun isAcceptableForAction(context: Context, intent: Intent): Boolean {
        if (!super.isAcceptableForAction(context, intent)) {
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // на версиях ниже N отсутствует Settings.Global.BOOT_COUNT
            return true
        }

        val lastBootCount = lastBootCount
            ?: return true // null - проверка не требуется

        // если значения совпадают, пропускаем событие
        return lastBootCount != getCurrentBootCount(context)
    }

    // вызвать super после основного doAction
    @CallSuper
    override fun doAction(context: Context, intent: Intent) {
        logger.d("doAction, intent: $intent")
        getCurrentBootCount(context)?.let {
            lastBootCount = it
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getBootCount(context: Context): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        } catch (e: Settings.SettingNotFoundException) {
            logException(logger, e, "Settings.Global.getInt")
            -1
        }
    }
}