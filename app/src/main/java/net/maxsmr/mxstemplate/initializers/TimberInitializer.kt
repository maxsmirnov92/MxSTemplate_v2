package net.maxsmr.mxstemplate.initializers

import android.content.Context
import androidx.startup.Initializer
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.timber.CustomFileLoggerTree
import timber.log.Timber
import android.util.Log
import fr.bipi.tressence.common.filters.TagFilter
import net.maxsmr.mxstemplate.timber.CrashReportingTree
import java.util.regex.Pattern

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
                CustomFileLoggerTree.Builder()
                    .withFileName("app_%g.log")
                    .withDir(context.externalCacheDir ?: context.cacheDir)
                    .withMinPriority(Log.DEBUG)
                    .withFilter(TagFilter(Pattern.compile("(TrackingStationService|TrackingInfo).*")))
                    .build()
            )
        }

//        Timber.plant(CrashReportingTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}
