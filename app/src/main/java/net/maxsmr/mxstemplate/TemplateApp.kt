package net.maxsmr.mxstemplate

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dagger.hilt.android.HiltAndroidApp
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.network.retrofit.client.BaseRetrofitClient
import net.maxsmr.core.network.retrofit.client.RadarIoRetrofitClient
import net.maxsmr.mxstemplate.logger.TimberLogger
import net.maxsmr.core.network.retrofit.client.okhttp.RadarIoOkHttpClientManager
import net.maxsmr.mxstemplate.manager.RadarIoHostManager
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class TemplateApp: Application(), Application.ActivityLifecycleCallbacks {

    init {
//        if (BuildConfig.DEBUG) {
//            System.setProperty(
//                kotlinx.coroutines.DEBUG_PROPERTY_NAME,
//                kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
//            )
//        }
    }

    @Inject
    @RadarIoRetrofit
    lateinit var radarIoRetrofitClient: RadarIoRetrofitClient

    init {
        BaseLoggerHolder.initInstance {
            object : BaseLoggerHolder() {

                override fun createLogger(className: String): BaseLogger = TimberLogger(className)
            }
        }

    }

    override fun onCreate() {
        super.onCreate()
//        FirebaseCrashlytics.getInstance().setUserId(UUIDManager.instance().uuid)

        registerActivityLifecycleCallbacks(this)
        radarIoRetrofitClient.init()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
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
    }
}