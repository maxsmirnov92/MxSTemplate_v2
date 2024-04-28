package net.maxsmr.mxstemplate.timber

import android.util.Log
//import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
//        return !(priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO)
        return false
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
//        val crashlytics = FirebaseCrashlytics.getInstance()
//
//        if (t == null) {
//            crashlytics.recordException(Exception(message))
//        } else {
//            crashlytics.recordException(t)
//        }
    }
}