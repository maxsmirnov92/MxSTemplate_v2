package net.maxsmr.mxstemplate

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dagger.hilt.android.HiltAndroidApp
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.retrofit.client.CommonRetrofitClient
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.mxstemplate.logger.TimberLogger
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks {

    init {
//        if (BuildConfig.DEBUG) {
//            System.setProperty(
//                kotlinx.coroutines.DEBUG_PROPERTY_NAME,
//                kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
//            )
//        }
        initLogging()
    }

    @Inject
    @YandexSuggestRetrofit
    lateinit var retrofitClient: CommonRetrofitClient

    private val _runningActivities = mutableListOf<Activity>()

    val runningActivities get() = _runningActivities.toList()

    override fun onCreate() {
        super.onCreate()
//        FirebaseCrashlytics.getInstance().setUserId(uuidManager.uuid)

        registerActivityLifecycleCallbacks(this)
        retrofitClient.init()

//        if (isAtLeastPie()) {
//            kotlin.runCatching {
//                WebView.setDataDirectorySuffix("chromeWebView")
//            }
//        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        _runningActivities.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        _runningActivities.remove(activity)
    }

    fun isActivityFirstAndSingle(clazz: Class<out BaseActivity>): Boolean {
        return runningActivities.indexOfFirst {
            it.componentName.className == clazz.canonicalName
        } == 0 && runningActivities.count {
            it.componentName.className == clazz.canonicalName
        } == 1
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