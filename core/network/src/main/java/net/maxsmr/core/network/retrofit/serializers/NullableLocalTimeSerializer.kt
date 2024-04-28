package net.maxsmr.core.network.retrofit.serializers

import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

internal object NullableLocalTimeSerializer : KSerializer<LocalTime?> {

    private val LOCAL_TIME: DateTimeFormatter = (java.time.format.DateTimeFormatterBuilder())
        .parseCaseInsensitive()
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .toFormatter()

    private val delegate = String.serializer().nullable

    override fun deserialize(decoder: Decoder): LocalTime? =
        delegate.deserialize(decoder)?.takeIf { it.isNotEmpty() }
            ?.let { java.time.LocalTime.parse(it, LOCAL_TIME).toKotlinLocalTime() }


    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "LocalDate",
        kind = STRING
    ).nullable

    override fun serialize(encoder: Encoder, value: LocalTime?) =
        if (value == null) {
            delegate.serialize(encoder, "")
        } else {
            delegate.serialize(encoder, value.toJavaLocalTime().format(LOCAL_TIME))
        }
}
