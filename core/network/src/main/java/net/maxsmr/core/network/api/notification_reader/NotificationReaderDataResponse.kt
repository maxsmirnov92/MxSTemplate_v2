package net.maxsmr.core.network.api.notification_reader

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseResponse

@Serializable(with = NotificationReaderDataResponse.Serializer::class)
data object NotificationReaderDataResponse : BaseResponse {

    override val errorCode = NO_ERROR_API

    override val errorMessage: String = EMPTY_STRING

    object Serializer : KSerializer<NotificationReaderDataResponse> {

        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("NotificationReaderDataResponse")

        override fun deserialize(decoder: Decoder): NotificationReaderDataResponse {
            return if (decoder is JsonDecoder) {
                decoder.decodeJsonElementOrNull()
                NotificationReaderDataResponse
            } else {
                throw SerializationException("This serializer only works with JSON")
            }

        }

        override fun serialize(encoder: Encoder, value: NotificationReaderDataResponse) {
            if (encoder is JsonEncoder) {
                encoder.encodeJsonElement(JsonObject(emptyMap()))
            } else {
                throw SerializationException("This serializer only works with JSON")
            }
        }

        private fun JsonDecoder.decodeJsonElementOrNull(): JsonElement? {
            return try {
                decodeJsonElement()
            } catch (e: SerializationException) {
                if (e.message?.contains("unexpected end of the input") == true) {
                    null
                } else {
                    throw e
                }
            }
        }
    }
}