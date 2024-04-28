package net.maxsmr.core.network.retrofit.internal.cache

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.*
import java.io.Flushable
import java.io.Closeable
import java.lang.reflect.Type
import kotlin.io.use
import kotlin.time.Duration.Companion.milliseconds

/**
 * Данный кэш хранит свои данные в каталоге в файловой системе устройства. Каталог должен использоваться только для
 * хранения данного кэша, т.к. кэш может удалять все файлы из своей директории.
 *
 * Данный кэш не имеет ограничений по объему занимаемой памяти.
 *
 * Каждый элемент кэша имеет временную отметку, по прошествии которой запись считается не валидной. Кроме этого, как
 * не валидные помечаются записи, связанные с файлами которые отсутсвуют, или не удалось выпольнить их десериализацию.
 *
 * Невалидные записи и все связанные с ними файлами удаляются из кэша.
 *
 * При первом обращении к кэшу происходит чтение и обработка журнала. Если журнал поврежден - текущий кэш удаляется
 * и создается пустой журнал.
 *
 */
class FileCache(
    fileSystem: FileSystem,
    private val directory: Path,
    private val json: Json,
    private val version: Int,
) : Closeable, Flushable {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThreadContext = Dispatchers.IO.limitedParallelism(1)//newSingleThreadContext("CacheContext")
    private var journalWriter: BufferedSink? = null
    private var hasJournalErrors: Boolean = false

    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var closed: Boolean = false

    private val fileSystem: FileSystem = object : ForwardingFileSystem(fileSystem) {
        override fun sink(file: Path, mustCreate: Boolean): Sink {
            file.parent?.let {
                createDirectories(it)
            }
            return super.sink(file, mustCreate)
        }
    }

    private val journalFile: Path = directory / JOURNAL_FILE
    private val journalFileTmp: Path = directory / JOURNAL_FILE_TEMP
    private val journalFileBackup: Path = directory / JOURNAL_FILE_BACKUP

    private val entries = HashMap<String, Entry>(256, 0.75f)

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalSerializationApi::class)
    private val entrySerializer: KSerializer<Entry> = serializer(Entry::class.java) as KSerializer<Entry>


    @Throws(FileNotFoundException::class)
    private fun newJournalWriter(): BufferedSink {
        val fileSink = fileSystem.appendingSink(journalFile)
        val faultHidingSink = FaultHidingSink(fileSink) {
            hasJournalErrors = true
        }
        return faultHidingSink.buffer()
    }

    @Throws(IOException::class)
    private fun initialize() {
        if (initialized) {
            return
        }

        if (fileSystem.exists(journalFileBackup)) {
            if (fileSystem.exists(journalFile)) {
                fileSystem.delete(journalFileBackup)
            } else {
                fileSystem.atomicMove(journalFileBackup, journalFile)
            }
        }

        try {
            if (fileSystem.exists(journalFile)) {
                fileSystem.read(journalFile) {
                    val version = readUtf8LineStrict().toInt()

                    if (version != this@FileCache.version) {
                        throw IOException("unexpected version: required $this@FileCache.version, current $version")
                    }

                    var lineCount = 0
                    while (true) {
                        try {
                            readJournalLine(readUtf8LineStrict()).let {
                                entries[it.key] = it
                            }
                            lineCount++
                        } catch (_: EOFException) {
                            break
                        }
                    }

                    if (lineCount > entries.size) {
                        rebuildJournal()
                    }

                    processJournal()
                }
            } else {
                rebuildJournal()
            }
        } catch (e: Exception) {
            deleteAndRebuild()
        }

        journalWriter = newJournalWriter()
        initialized = true
        closed = false
    }

    @Throws(IOException::class)
    private fun readJournalLine(line: String): Entry = json.decodeFromString(entrySerializer, line)

    @Throws(IOException::class)
    private fun processJournal() {
        fileSystem.deleteIfExists(journalFileTmp)

        val i = entries.iterator()
        val now = Clock.System.now()
        var originalSize = entries.size

        while (i.hasNext()) {
            val entry = i.next()

            if (entry.value.expired || now > entry.value.expiresAt) {
                fileSystem.deleteIfExists(directory / entry.value.key)
                i.remove()
            }
        }

        if (originalSize > entries.size) {
            rebuildJournal()
        }
    }

    private fun rebuildJournal() {
        journalWriter?.close()

        fileSystem.write(journalFileTmp) {
            writeDecimalLong(version.toLong()).writeByte('\n'.code)

            for (entry in entries) {
                writeUtf8(json.encodeToString(entrySerializer, entry.value))
                writeByte('\n'.code)
            }
        }

        if (fileSystem.exists(journalFile)) {
            fileSystem.atomicMove(journalFile, journalFileBackup)
            fileSystem.atomicMove(journalFileTmp, journalFile)
            fileSystem.deleteIfExists(journalFileBackup)
        } else {
            fileSystem.atomicMove(journalFileTmp, journalFile)
        }

        journalWriter = newJournalWriter()
    }

    /**
     * Удаление всех файлов из директории кэша
     */
    @Throws(IOException::class)
    private fun deleteAndRebuild() {
        close()
        fileSystem.deleteContents(directory)
        rebuildJournal()
    }

    suspend fun clear() = withContext(singleThreadContext) {
        deleteAndRebuild()
    }

    @ExperimentalSerializationApi
    private fun serializer(type: Type): KSerializer<Any> = json.serializersModule.serializer(type)

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(IOException::class)
    suspend fun get(key: String, type: Type, strictTimeLimitInMillis: Long = 0): Any? = withContext(singleThreadContext) {
        initialize()

        val entry = entries[key] ?: return@withContext null
        val now = Clock.System.now()

        return@withContext if (
                !entry.expired //Запись валидна
                && entry.expiresAt > now //Запись не устарела
                && (entry.expiresAt.toEpochMilliseconds() - Clock.System.now()
                    .toEpochMilliseconds()) <= entry.maxAgeMillis //Запись находится внутри допустимого интервала
                && fileSystem.exists(
                    directory / entry.key
                )
        ) {
            if (strictTimeLimitInMillis > 0 && (Clock.System.now() - entry.createdTime) > strictTimeLimitInMillis.milliseconds) {
                 return@withContext null
            }

            try {
                json.decodeFromBufferedSource(
                    serializer(type),
                    fileSystem.source(directory / entry.key).buffer()
                )
            } catch (e: Exception) {
                //Не удалось распарсить файл, удаляем данные
                removeEntry(entry)
                null
            }
        } else {
            //Запись не валидна, удаляем данные
            removeEntry(entry)
            null
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    suspend fun <T : @Serializable Any> put(entry: Entry, obj: T, type: Type) = coroutineScope {
        withContext(singleThreadContext) {
            initialize()

            entries[entry.key] = entry
            logToJournal(entry)

            fileSystem.sink(directory / entry.key).buffer().use {
                json.encodeToBufferedSink(serializer(type), obj, it)
            }
        }
    }

    private fun removeEntry(entry: Entry) {
        logToJournal(entry.copy(expired = true))

        fileSystem.deleteIfExists(directory / entry.key)
        entries.remove(entry.key)
    }

    private fun logToJournal(entry: Entry) {
        journalWriter!!
            .writeUtf8(json.encodeToString(entrySerializer, entry))
            .writeByte('\n'.code)
            .flush()
    }

    @Serializable
    data class Entry(
        val key: String,
        val expiresAt: Instant,
        val maxAgeMillis: Long,
        val expired: Boolean,
        val createdTime: Instant,
    ) {

        class Factory(
            val key: String,
            private val now: Instant,
            private val maxAgeMillis: Long,
        ) {

            fun create(): Entry {
                return Entry(key, now.plus(maxAgeMillis, DateTimeUnit.MILLISECOND), maxAgeMillis, false, now)
            }
        }
    }

    override fun close() {
        if (!initialized || closed) {
            closed = true
            return
        }

        journalWriter!!.close()
        journalWriter = null
        closed = true
    }

    override fun flush() {
        if (!initialized) return

        if (!closed) {
            journalWriter!!.flush()
        }
    }


    companion object {

        const val JOURNAL_FILE = "journal"
        const val JOURNAL_FILE_TEMP = "journal.tmp"
        const val JOURNAL_FILE_BACKUP = "journal.bkp"
    }
}