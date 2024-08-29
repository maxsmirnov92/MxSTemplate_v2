package net.maxsmr.address_sorter.initializers

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import fr.bipi.tressence.common.filters.TagFilter
import net.maxsmr.address_sorter.BuildConfig
import net.maxsmr.address_sorter.timber.CrashReportingTree
import net.maxsmr.address_sorter.timber.CustomFileLoggerTree
import timber.log.Timber
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
