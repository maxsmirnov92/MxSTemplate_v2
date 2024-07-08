package net.maxsmr.justupdownloadit.initializers

import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber
import android.util.Log
import dagger.hilt.EntryPoint
import fr.bipi.tressence.common.filters.TagFilter
import net.maxsmr.justupdownloadit.BuildConfig
import net.maxsmr.justupdownloadit.timber.CrashReportingTree
import net.maxsmr.justupdownloadit.timber.CustomFileLoggerTree
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

        Timber.plant(CrashReportingTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}
