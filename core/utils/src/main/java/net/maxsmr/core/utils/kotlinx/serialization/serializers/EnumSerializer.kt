package net.maxsmr.core.utils.kotlinx.serialization.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.maxsmr.core.domain.IntId
import net.maxsmr.core.domain.StringId
import kotlin.reflect.KClass

open class IntIdEnumSerializer<T>(
    kClass: KClass<T>,
    private val values: Array<T>,
    private val defaultValue: T
) : KSerializer<T> where T : Enum<T>, T : IntId {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        kClass.simpleName ?: "IntEnum", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(value.id)
    }

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeInt()

        return values.firstOrNull { it.id == value } ?: defaultValue
    }
}

open class StringIdEnumSerializer<T>(
    kClass: KClass<T>,
    private val values: Array<T>,
    private val defaultValue: T,
    private val ignoreCase: Boolean = true
) : KSerializer<T> where T : Enum<T>, T : StringId {


    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        kClass.simpleName ?: "IntEnum", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeString()

        return values.firstOrNull { it.id.equals(value, ignoreCase) } ?: defaultValue
    }
}
