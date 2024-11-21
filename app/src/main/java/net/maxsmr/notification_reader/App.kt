package net.maxsmr.notification_reader

import dagger.hilt.android.HiltAndroidApp
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.ui.components.BaseApplication
import net.maxsmr.notification_reader.logger.TimberLogger

@HiltAndroidApp
class App : BaseApplication() {

    private val logger by lazy { BaseLoggerHolder.instance.getLogger<BaseLogger>("App") }

    init {
        initLogging()
    }


    override fun onCreate() {
        super.onCreate()
        logger.d("onCreate")
//        FirebaseCrashlytics.getInstance().setUserId(uuidManager.uuid)
    }

    private fun initLogging() {
        BaseLoggerHolder.init {
            object : BaseLoggerHolder() {

                override fun createLogger(className: String): BaseLogger = TimberLogger(className)
            }
        }
        BaseLoggerHolder.instance.loggerLevel = if (BuildConfig.DEBUG) {
            BaseLogger.Level.VERBOSE
        } else {
            BaseLogger.Level.INFO
        }
    }
}