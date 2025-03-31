package net.maxsmr.core.network.exceptions

import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.IOException

/**
 * Вспомогательный [IOException], содержащий исходный
 */
class OkHttpException(cause: Throwable) : IOException(cause.message, cause) {

    companion object {

        @JvmStatic
        @JvmOverloads
        internal fun Throwable.orNetworkCause(message: String = EMPTY_STRING): RuntimeException {
            return (if (this is OkHttpException) {
                this.cause
            } else {
                this
            }) as? RuntimeException
                ?: NetworkException(cause = this.cause, message = message)
        }
    }
}