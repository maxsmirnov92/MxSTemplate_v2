package net.maxsmr.notification_reader.initializers

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import fr.bipi.treessence.file.FileLoggerTree
import net.maxsmr.notification_reader.BuildConfig
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Timber.plant(object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String {
                return String.format(
                    "%s.%s(%s)",
                    super.createStackElementTag(element),
                    element.methodName,
                    element.lineNumber
                )
            }
        })
        if (BuildConfig.LOG_WRITE_FILE) {
            Timber.plant(
                FileLoggerTree.Builder()
                    .withFileName("app_%g.log")
                    .withDir(context.externalCacheDir ?: context.cacheDir)
                    .withMinPriority(Log.VERBOSE)
//                    .withFilter(TagFilter(Pattern.compile("")))
                    .build()
            )
        }

//        Timber.plant(CrashReportingTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}