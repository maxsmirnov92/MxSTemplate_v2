package net.maxsmr.feature.download.data.storage

import androidx.core.net.toUri

/**
 * Исключение, возникающее при выполнении операции с [localUri]
 */
class StoreException @JvmOverloads constructor(
    private val _localUri: String,
    cause: Exception? = null,
    message: String = cause?.message.orEmpty()
) : RuntimeException(message, cause) {

    val localUri get() = _localUri.toUri()
}