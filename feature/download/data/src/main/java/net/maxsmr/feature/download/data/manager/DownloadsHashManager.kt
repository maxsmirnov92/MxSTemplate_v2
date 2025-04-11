package net.maxsmr.feature.download.data.manager

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.commonutils.ALGORITHM_SHA1
import net.maxsmr.commonutils.digest
import net.maxsmr.commonutils.toHexString
import net.maxsmr.core.domain.entities.feature.download.HashInfo
import javax.inject.Inject
import javax.inject.Singleton

private const val ALGORITHM_DEFAULT = ALGORITHM_SHA1

@Singleton
class DownloadsHashManager @Inject constructor(@ApplicationContext private val context: Context) {

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
            uri.digest(context.contentResolver, algorithm).toHexString()
        )
    }
}