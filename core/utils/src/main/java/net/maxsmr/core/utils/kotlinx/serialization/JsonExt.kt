package net.maxsmr.core.utils.kotlinx.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

val jsonExtLogger: BaseLogger = BaseLoggerHolder.instance.getLogger("JsonExt")

inline fun <reified T> Json.encodeToStringOrNull(data: T): String? {
    return try {
        encodeToString(serializersModule.serializer(), data)
    } catch (e: Exception) {
        jsonExtLogger.e(e)
        null
    }
}

inline fun <reified T> Json.decodeFromStringOrNull(data: String?): T? {
    data ?: return null
    return try {
        decodeFromString<T>(serializersModule.serializer(), data)
    } catch (e: Exception) {
        jsonExtLogger.e(e)
        null
    }
}