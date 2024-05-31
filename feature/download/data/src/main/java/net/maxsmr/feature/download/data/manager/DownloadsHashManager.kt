package net.maxsmr.feature.download.data.manager

import android.net.Uri
import net.maxsmr.commonutils.ALGORITHM_SHA1
import net.maxsmr.commonutils.digest
import net.maxsmr.commonutils.toHexString
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.domain.entities.feature.download.HashInfo

object DownloadsHashManager {

    private const val ALGORITHM_DEFAULT = ALGORITHM_SHA1

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
}