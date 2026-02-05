package net.maxsmr.feature.download.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import net.maxsmr.commonutils.media.delete
import net.maxsmr.commonutils.text.getExtension
import net.maxsmr.core.android.baseAppName
import net.maxsmr.feature.download.data.DownloadService
import java.io.File
import java.io.OutputStream

abstract class DownloadServiceStorage(
    val type: Type,
    protected val context: Context,
) {

    protected open val baseAppDir: String = baseAppName

    protected val contentResolver: ContentResolver by lazy {
        context.contentResolver
    }

    /**
     * Сохраняет загруженный файл в память девайса
     *
     * @param params параметры загрузки
     * @param targetUri целевая uri для сохранения файла. Если не задан, создается новая.
     * Не null, если с прошлой загрузки остались битые файлы, которые можно перетереть новой текущей загрузкой.
     */
    abstract fun store(
        params: DownloadService.Params,
        targetUri: Uri?,
        writeStreamFunc: (Uri, OutputStream, Long) -> Unit,
    ): Uri

    protected abstract fun namesAt(fullDirPath: String, startsWith: String?): Set<UriAndName>

    /**
     * Возвращает список uri файлов, префикс названий которых совпадает с префиксом [DownloadService.Params.resourceName].
     * Префикс - это название ресурса до уникальной цифры (1) либо до расширения файла.
     */
    fun alreadyLoadedUris(params: DownloadService.Params): List<Uri> {
        val dirPath = type.dirPath(context, baseAppDir, params.subDirPath)
        val baseNamePart = baseNamePart(params.targetResourceName)
        return namesAt(dirPath, baseNamePart).map { it.uri }
    }

    protected fun uniqueNameFor(fullDirPath: String, srcName: String): String {

        fun uniquePostfix(index: Int): String =
            "$UNIQUE_NAME_PREFIX$index$UNIQUE_NAME_SUFFIX"

        fun appendIndexTo(name: String, index: Int): String {
            val dotIndex = name.lastIndexOf('.')
            return if (dotIndex == -1) {
                "$name ${uniquePostfix(index)}"
            } else {
                "${name.substringBeforeLast('.')} ${uniquePostfix(index)}." +
                        name.getExtension()
            }
        }

        fun nextUniqueIndex(names: List<String>): Int? {
            val indexNone = -1
            val existingIndices = mutableSetOf<Int>()
            for (n in names) {
                val startIndex = n.lastIndexOf(UNIQUE_NAME_PREFIX)
                if (startIndex == -1) {
                    existingIndices.add(indexNone)
                    continue
                }
                val endIndex = n.lastIndexOf(UNIQUE_NAME_SUFFIX)
                if (endIndex == -1) continue
                n.substring(startIndex + 1, endIndex).toIntOrNull()?.let(existingIndices::add)
            }
            if (!existingIndices.contains(indexNone)) return null
            var i = 1
            while (i in existingIndices.sorted()) {
                i++
            }
            return i
        }

        val existingNames = namesAt(fullDirPath, baseNamePart(srcName))
        val index = nextUniqueIndex(existingNames.map { it.name })
        return if (existingNames.isEmpty() || index == null) srcName else appendIndexTo(srcName, index)
    }

    private fun baseNamePart(name: String): String {
        name.lastIndexOf(UNIQUE_NAME_PREFIX).takeIf { it != -1 }?.let {
            return name.take(it)
        }
        name.lastIndexOf('.').takeIf { it != -1 }?.let {
            return name.take(it)
        }
        return name
    }

    /**
     * Пытается удалить ресурс по [uri] в случае возникновения исключения при сохранении. Если удалить
     * не удалось, оборачивает исключение, добавляя эту uri, чтобы в будущем можно было попробовать
     * переписать битый файл по этой [uri].
     */
    protected fun Exception.wrapIfNeed(
        uri: Uri?,
        deleteUnfinished: Boolean
    ): Exception {
        if (uri == null || !deleteUnfinished) {
            return this
        }
        return if (uri.delete(contentResolver)) this else StoreException(uri.toString(), this)
    }

    protected data class UriAndName(
        val uri: Uri,
        val name: String,
    )


    @Suppress("unused")
    enum class Type {

        CACHE {

            override fun rootDirPath(context: Context): String {
                return context.cacheDir.absolutePath
            }
        },

        FILES {

            override fun rootDirPath(context: Context): String {
                return context.filesDir.absolutePath
            }
        },
        
        EXTERNAL {

            override fun rootDirPath(context: Context): String {
                return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
                    ?: throw RuntimeException("External storage unavailable")
            }
        },
        
        SHARED {

            override fun rootDirPath(context: Context): String {
                return if (useMediaStore()) {
                    Environment.DIRECTORY_DOWNLOADS
                }
                else {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                }
            }
        };

        val isInternal: Boolean get() = this in listOf(CACHE, FILES)

        fun dirPath(
            context: Context,
            baseAppDir: String,
            subDirPath: String?,
        ): String {
            val rootPath = rootDirPath(context)
            val dirPath = if (subDirPath.isNullOrEmpty()) baseAppDir else baseAppDir + File.separator + subDirPath
            return dirPath.removePrefix(File.separator).takeIf { it.isNotBlank() }?.let {
                rootPath + File.separator + it + File.separator
            } ?: (rootPath + File.separator)
        }

        protected abstract fun rootDirPath(context: Context): String
    }

    companion object {

        internal const val UNIQUE_NAME_PREFIX = '('
        internal const val UNIQUE_NAME_SUFFIX = ')'

        fun create(
            context: Context,
            type: Type,
        ): DownloadServiceStorage {
            return if (useMediaStore() && type == Type.SHARED) {
                MediaStoreStorage(context)
            } else {
                FileStorage(type, context)
            }
        }

        private fun useMediaStore(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}