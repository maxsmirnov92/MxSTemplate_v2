package net.maxsmr.justupdownloadit

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.ui.components.BaseApplication
import net.maxsmr.justupdownloadit.logger.TimberLogger
import net.maxsmr.justupdownloadit.manager.UUIDManager
import javax.inject.Inject

@HiltAndroidApp
class App : BaseApplication() {

    private val logger by lazy { BaseLoggerHolder.instance.getLogger<BaseLogger>("App") }

    @Inject
    lateinit var uuidManager: UUIDManager

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
        logger.d("onCreate")

        val uuid = uuidManager.uuid
        FirebaseCrashlytics.getInstance().setUserId(uuid)
        FirebaseAnalytics.getInstance(this).apply {
            setUserId(uuid)
            logEvent("AppOnCreate", null)
        }

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