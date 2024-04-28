package net.maxsmr.core.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.util.Date

val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("KotlinxUtils")

inline fun <reified T> Json.encodeToStringOrNull(data: T): String? {
    return try {
        encodeToString(serializersModule.serializer(), data)
    } catch (e: Exception) {
        logger.e(e)
        null
    }
}

inline fun <reified T> Json.decodeFromStringOrNull(data: String?): T? {
    data ?: return null
    return try {
        decodeFromString<T>(serializersModule.serializer(), data)
    } catch (e: Exception) {
        logger.e(e)
        null
    }
}

class PolymorphicEnumSerializer<T: Enum<*>>(private val enumSerializer: KSerializer<T>) :
        KSerializer<T> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(enumSerializer.descriptor.serialName) {
            element("enumType", enumSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): T =
        decoder.decodeStructure(descriptor) {
            decodeElementIndex(descriptor)
            decodeSerializableElement(descriptor, 0, enumSerializer)
        }

    override fun serialize(encoder: Encoder, value: T) =
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, enumSerializer, value)
        }
}

object DateAsLongSerializer : KSerializer<Date> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)

    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}