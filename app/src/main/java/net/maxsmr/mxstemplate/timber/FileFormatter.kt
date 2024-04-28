package net.maxsmr.mxstemplate.timber

import android.util.Log
import fr.bipi.tressence.common.formatter.Formatter
import fr.bipi.tressence.common.os.OsInfoProvider
import fr.bipi.tressence.common.os.OsInfoProviderDefault
import fr.bipi.tressence.common.time.TimeStamper

class FileFormatter : Formatter {

    private val priorities = mapOf(
        Log.VERBOSE to "VERBOSE",
        Log.DEBUG   to "DEBUG",
        Log.INFO    to " INFO",
        Log.WARN    to " WARN",
        Log.ERROR   to "ERROR",
        Log.ASSERT  to "ASSERT"
    )

    var timeStamper = TimeStamper("dd/MM/yyyy HH:mm:ss,SSS")
    var osInfoProvider: OsInfoProvider = OsInfoProviderDefault()

    override fun format(priority: Int, tag: String?, message: String): String {
        return "${timeStamper.getCurrentTimeStamp(osInfoProvider.currentTimeMillis)} ${priorities[priority] ?: ""} ${tag ?: ""} - ${message}\n"
    }

    companion object {
        val INSTANCE = FileFormatter()
    }
}