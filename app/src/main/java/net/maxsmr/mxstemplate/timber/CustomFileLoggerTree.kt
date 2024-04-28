package net.maxsmr.mxstemplate.timber

import fr.bipi.tressence.base.FormatterPriorityTree
import fr.bipi.tressence.common.filters.Filter
import fr.bipi.tressence.common.filters.NoFilter
import fr.bipi.tressence.common.formatter.Formatter
import java.util.logging.FileHandler
import android.util.Log
import fr.bipi.tressence.base.NoTree
import fr.bipi.tressence.common.utils.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
    Класс является копией fr.bipi.treessence.file.FileLoggerTree, т.к. на текущий момент нет другой
    возможности переопределить метод createStackElementTag

    Убрать если автор библиотеки внесет исправления
    https://github.com/bastienpaulfr/Treessence/issues/12
 */

class CustomFileLoggerTree(
    private val logger: Logger,
    private val fileHandler: FileHandler?,
    private val path: String,
    private val nbFiles: Int,
    priority: Int,
    filter: Filter = NoFilter.INSTANCE,
    formatter: Formatter = FileFormatter.INSTANCE
) : FormatterPriorityTree(priority, filter, formatter) {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (skipLog(priority, tag, message, t)) {
            return
        }
        logger.log(fromPriorityToLevel(priority), format(priority, tag, message))
        if (t != null) {
            logger.log(fromPriorityToLevel(priority), "", t)
        }
    }

    /**
     * Delete all log files
     */
    fun clear() {
        fileHandler?.close()
        for (i in 0 until nbFiles) {
            val f = File(getFileName(i))
            if (f.exists() && f.isFile) {
                f.delete()
            }
        }
    }

    /**
     * Return the file name corresponding to the number
     *
     * @param i Number of file
     * @return Real file name
     */
    fun getFileName(i: Int): String {
        return if (!this.path.contains("%g")) {
            this.path + "." + i
        } else {
            this.path.replace("%g", "" + i)
        }
    }

    /**
     * @return All files created by the logger
     */
    val files: Collection<File>
        get() {
            val col: MutableCollection<File> = ArrayList(nbFiles)
            for (i in 0 until nbFiles) {
                val f = File(getFileName(i))
                if (f.exists()) {
                    col.add(f)
                }
            }
            return col
        }

    private fun fromPriorityToLevel(priority: Int): Level {
        return when (priority) {
            Log.VERBOSE -> Level.FINER
            Log.DEBUG -> Level.FINE
            Log.INFO -> Level.INFO
            Log.WARN -> Level.WARNING
            Log.ERROR -> Level.SEVERE
            Log.ASSERT -> Level.SEVERE
            else -> Level.FINEST
        }
    }

    override fun createStackElementTag(element: StackTraceElement): String {
        return String.format(
            "%s.%s:%s",
            super.createStackElementTag(element),
            element.methodName,
            element.lineNumber
        )
    }

    class Builder {
        private var fileName = "log"
        private var dir = ""
        private var priority = Log.INFO
        private var sizeLimit = SIZE_LIMIT
        private var fileLimit = NB_FILE_LIMIT
        private var appendToFile = true
        private var filter: Filter = NoFilter.INSTANCE
        private var formatter: Formatter = FileFormatter.INSTANCE

        /**
         * Specify a custom file name
         *
         *  Default file name is "log" which will result in log.0, log.1, log.2...
         *
         * @param fn File name
         * @return itself
         */
        fun withFileName(fn: String): Builder {
            fileName = fn
            return this
        }

        /**
         * Specify a custom dir name
         *
         * @param dn Dir name
         * @return itself
         */
        fun withDirName(dn: String): Builder {
            dir = dn
            return this
        }

        /**
         * Specify a custom dir name
         *
         * @param d Dir file
         * @return itself
         */
        fun withDir(d: File): Builder {
            dir = d.absolutePath
            return this
        }

        /**
         * Specify a priority from which it can start logging
         *
         *  Default is [Log.INFO]
         *
         * @param p priority
         * @return itself
         */
        fun withMinPriority(p: Int): Builder {
            priority = p
            return this
        }

        /**
         * Specify a custom file size limit
         *
         *  Default is 1048576 bytes
         *
         * @param nbBytes Custom size limit in bytes
         * @return itself
         */
        fun withSizeLimit(nbBytes: Int): Builder {
            sizeLimit = nbBytes
            return this
        }

        /**
         * Specify a custom file number limit
         *
         *  Default is 3
         *
         * @param f Max number of files to use
         * @return itself
         */
        fun withFileLimit(f: Int): Builder {
            fileLimit = f
            return this
        }

        /**
         * Specify a log filter
         *
         * @param filter Log filter
         */
        fun withFilter(filter: Filter): Builder {
            this.filter = filter
            return this
        }

        /**
         * Specify a log formatter
         *
         * @param formatter Log formatter
         */
        fun withFormatter(formatter: Formatter): Builder {
            this.formatter = formatter
            return this
        }

        /**
         * Specify an option for [FileHandler] creation
         *
         * @param b true to append to existing file
         * @return itself
         */
        fun appendToFile(b: Boolean): Builder {
            appendToFile = b
            return this
        }

        /**
         * Create the file logger tree with options specified.
         *
         * @return [FileLoggerTree]
         * @throws IOException if file creation fails
         */
        @Throws(IOException::class)
        fun build(): CustomFileLoggerTree {
            // 1st create dir if it does not exists
            File(dir).apply {
                if (!isDirectory) {
                    mkdirs()
                }
            }

            val path = FileUtils.combinePath(dir, fileName)
            val fileHandler: FileHandler
            val logger = MyLogger.getLogger(TAG)
            logger.level = Level.ALL
            if (logger.handlers.isEmpty()) {
                fileHandler = FileHandler(path, sizeLimit, fileLimit, appendToFile)
                fileHandler.formatter = NoFormatter()
                logger.addHandler(fileHandler)
            } else {
                fileHandler = logger.handlers[0] as FileHandler
            }
            return CustomFileLoggerTree(logger, fileHandler, path, fileLimit, priority, filter, formatter)
        }

        /**
         * Create the file logger tree with options specified.
         *
         * @return [FileLoggerTree] or [NoTree] if an exception occurred
         */
        fun buildQuietly(): Timber.Tree {
            return try {
                build()
            } catch (e: IOException) {
                Timber.e(e)
                NoTree()
            }
        }

        companion object {
            const val SIZE_LIMIT = 1048576
            const val NB_FILE_LIMIT = 3
        }
    }

    private class NoFormatter : java.util.logging.Formatter() {
        override fun format(record: LogRecord): String {
            return record.message
        }
    }

    /**
     * Custom logger class that has no references to LogManager
     *
     * Constructs a `Logger` object with the supplied name and resource
     * bundle name; `notifyParentHandlers` is set to `true`.
     *
     *
     * Notice : Loggers use a naming hierarchy. Thus "z.x.y" is a child of "z.x".
     *
     * @param name the name of this logger, may be `null` for anonymous
     * loggers.
     */
    private class MyLogger(name: String?) : Logger(name, null) {
        companion object {
            fun getLogger(name: String?): Logger {
                return MyLogger(name)
            }
        }
    }

    companion object {
        private const val TAG = "FileLoggerTree"
    }
}