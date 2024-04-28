package net.maxsmr.core.network.retrofit.serializers

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

internal object LocalDateSerializer : KSerializer<LocalDate> {

    val LOCAL_DATE: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral('.')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('.')
        .appendValue(ChronoField.YEAR, 4, 10, java.time.format.SignStyle.EXCEEDS_PAD)
        .toFormatter()

    override fun deserialize(decoder: Decoder): LocalDate =
        java.time.LocalDate.parse(decoder.decodeString(), LOCAL_DATE).toKotlinLocalDate()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "LocalDate",
        kind = STRING
    )

    override fun serialize(encoder: Encoder, value: LocalDate) =
        encoder.encodeString(value.toJavaLocalDate().format(LOCAL_DATE))
}

internal object NullableLocalDateSerializer : KSerializer<LocalDate?> {

    private val LOCAL_DATE: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral('.')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('.')
        .appendValue(ChronoField.YEAR, 4, 10, java.time.format.SignStyle.EXCEEDS_PAD)
        .toFormatter()

    private val delegate = String.serializer().nullable
    override fun deserialize(decoder: Decoder): LocalDate? =
        delegate.deserialize(decoder)?.takeIf { it.isNotEmpty() }
            ?.let { java.time.LocalDate.parse(it, LOCAL_DATE).toKotlinLocalDate()}

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "LocalDate",
        kind = STRING
    )

    override fun serialize(encoder: Encoder, value: LocalDate?) =
        if (value == null) {
            delegate.serialize(encoder, "")
        } else {
            delegate.serialize(encoder, value.toJavaLocalDate().format(
                LOCAL_DATE
            ))
        }
}

internal object LocalDateListSerializer : KSerializer<List<LocalDate>> {

    private val listSerializer = ListSerializer(LocalDateSerializer)

    override val descriptor: SerialDescriptor
        get() = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<LocalDate> {
        return listSerializer.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: List<LocalDate>) {
        return listSerializer.serialize(encoder, value)
    }
}