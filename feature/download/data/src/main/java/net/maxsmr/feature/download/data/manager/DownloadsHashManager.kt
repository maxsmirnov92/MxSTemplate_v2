package net.maxsmr.feature.download.data.manager

import android.net.Uri
import net.maxsmr.commonutils.digest
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.domain.entities.feature.download.HashInfo
import net.maxsmr.core.domain.entities.feature.download.HashInfo.Companion.ALGORITHM_SHA1
import java.util.Locale

object DownloadsHashManager {

    private const val ALGORITHM_DEFAULT = ALGORITHM_SHA1

    private const val HEX_CHARS = "0123456789ABCDEF"

    /**
     * Проверяет соответствие хэша ресурса с адресом [uri] ожидаемому хэшу [expected]
     * @return true, если хэши совпадают, иначе false
     */
    fun checkHash(uri: Uri, expected: HashInfo): Boolean {
        val algorithm = expected.algorithm
        val hash = getHash(uri, algorithm).takeIf { !it.isEmpty } ?: return false
        return expected == hash
    }

    fun getHash(uri: Uri, algorithm: String = ALGORITHM_DEFAULT): HashInfo {
        return HashInfo(
            algorithm,
            uri.digest(baseApplicationContext.contentResolver, algorithm).toHexString()
        )
    }

    private fun ByteArray?.toHexString(): String {
        this ?: return EMPTY_STRING

        val result = StringBuilder(size * 2)
        forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }
        return result.toString().lowercase(Locale.getDefault())
    }
}