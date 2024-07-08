package net.maxsmr.justupdownloadit

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.ui.components.BaseApplication
import net.maxsmr.justupdownloadit.logger.TimberLogger

@HiltAndroidApp
class App : BaseApplication() {

    init {
//        if (BuildConfig.DEBUG) {
//            System.setProperty(
//                kotlinx.coroutines.DEBUG_PROPERTY_NAME,
//                kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
//            )
//        }
        initLogging()
    }


    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setUserId(uuidManager.uuid)

//        if (isAtLeastPie()) {
//            kotlin.runCatching {
//                WebView.setDataDirectorySuffix("chromeWebView")
//            }
//        }
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