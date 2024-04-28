package net.maxsmr.core.network.retrofit.serializers

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

internal object TimeStampSerializer : KSerializer<Instant> {

    private val LOCAL_DATE_TIME: DateTimeFormatter = (java.time.format.DateTimeFormatterBuilder())
        .parseCaseInsensitive()
        .appendValue(ChronoField.YEAR, 4, 10, java.time.format.SignStyle.EXCEEDS_PAD)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral(' ')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .toFormatter()

    override fun deserialize(decoder: Decoder): Instant =
        java.time.LocalDateTime.parse(decoder.decodeString(), LOCAL_DATE_TIME).toKotlinLocalDateTime()
            .toInstant(TimeZone.of("Europe/Moscow"))

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "LocalDate",
        kind = STRING
    )

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(value.toLocalDateTime(TimeZone.of("Europe/Moscow")).toJavaLocalDateTime()
            .format(LOCAL_DATE_TIME))
}
