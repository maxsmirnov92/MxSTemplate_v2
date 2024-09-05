package net.maxsmr.core.network.exceptions

import java.io.IOException

/**
 * Вспомогательный [IOException], содержащий исходный
 */
class OkHttpException(cause: Throwable): IOException(cause)