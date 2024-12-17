package net.maxsmr.core.network.retrofit.serializers

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.maxsmr.core.network.retrofit.serializers.LocalDateSerializer.LOCAL_DATE
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

@RequiresApi(Build.VERSION_CODES.O)
internal object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    private val LOCAL_DATE_TIME: DateTimeFormatter = (java.time.format.DateTimeFormatterBuilder())
        .parseCaseInsensitive()
        .append(LOCAL_DATE)
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

    override fun deserialize(decoder: Decoder): LocalDateTime =
        java.time.LocalDateTime.parse(decoder.decodeString(), LOCAL_DATE_TIME).toKotlinLocalDateTime()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "LocalDate",
        kind = STRING
    )

    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeString(value.toJavaLocalDateTime().format(LOCAL_DATE_TIME))
}
