package net.maxsmr.core.network.retrofit.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.maxsmr.core.domain.IntId
import kotlin.reflect.KClass


open class IntEnumSerializer<T>(
    kClass: KClass<T>,
    private val values: Array<T>,
    private val defaultValue: T
) : KSerializer<T> where T : Enum<T>, T : IntEnum {


    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        kClass.simpleName ?: "IntEnum", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeInt()

        return values.firstOrNull { it.value == value } ?: defaultValue
    }
}

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


interface IntEnum {

    val value: Int
}