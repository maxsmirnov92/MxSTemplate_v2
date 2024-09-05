package net.maxsmr.core.ui.components

import android.app.Activity
import android.app.Application
import android.os.Bundle
import net.maxsmr.core.ui.components.activities.BaseActivity

abstract class BaseApplication: Application(), Application.ActivityLifecycleCallbacks {

    private val _runningActivities = mutableListOf<Activity>()

    val runningActivities get() = _runningActivities.toList()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
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
}