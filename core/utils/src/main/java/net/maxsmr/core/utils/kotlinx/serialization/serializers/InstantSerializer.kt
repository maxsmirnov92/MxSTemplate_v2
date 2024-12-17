package net.maxsmr.core.utils.kotlinx.serialization.serializers

import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantSerializer : KSerializer<Instant> {

    override fun deserialize(decoder: Decoder): Instant =
        fixOffsetRepresentation(decoder.decodeString()).toInstant()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "Instant",
        kind = STRING
    )

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(value.toString())

    private fun fixOffsetRepresentation(isoString: String): String {
        val time = isoString.indexOf('T', ignoreCase = true)
        if (time == -1) return isoString // the string is malformed
        val offset = isoString.indexOfLast { c -> c == '+' || c == '-' }
        if (offset < time) return isoString // the offset is 'Z' and not +/- something else
        val separator = isoString.indexOf(':', offset) // if there is a ':' in the offset, no changes needed

        val offsetLength = isoString.length - (offset + 1)

        return if (separator != -1) isoString else if (offsetLength == 2) "$isoString:00" else "${
            isoString.substring(
                0,
                isoString.length - 2
            )
        }:${isoString.substring(isoString.length - 2)}"
    }
}
