package net.maxsmr.core.network.retrofit.serializers

import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

internal object LocalTimeSerializer : KSerializer<LocalTime> {

    private val LOCAL_TIME: DateTimeFormatter = (java.time.format.DateTimeFormatterBuilder())
        .parseCaseInsensitive()
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .toFormatter()

    override fun deserialize(decoder: Decoder): LocalTime =
            java.time.LocalTime.parse(decoder.decodeString(), LOCAL_TIME).toKotlinLocalTime()


    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "LocalDate",
        kind = STRING
    )

    override fun serialize(encoder: Encoder, value: LocalTime) =
        encoder.encodeString(value.toJavaLocalTime().format(LOCAL_TIME))
}
