package net.maxsmr.core.utils.kotlinx.serialization.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

class PolymorphicEnumSerializer<T : Enum<*>>(private val enumSerializer: KSerializer<T>) :
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