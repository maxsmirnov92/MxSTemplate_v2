package net.maxsmr.feature.download.data.storage

import android.net.Uri

/**
 * Исключение, возникающее при выполнении операции с [localUri]
 */
class StoreException(
    val localUri: Uri,
    cause: Exception? = null,
    message: String = cause?.message.orEmpty()
) : RuntimeException(message, cause) {

    constructor(localUri: Uri, message: String) : this(localUri, null, message)
}