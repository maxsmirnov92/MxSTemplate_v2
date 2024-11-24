package net.maxsmr.core.network.client.retrofit

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import net.maxsmr.core.network.retrofit.converters.BaseEnvelopeWithObject
import net.maxsmr.core.network.retrofit.converters.EnvelopeObjectTypeConverter
import net.maxsmr.core.network.retrofit.converters.api.YandexGeocodeEnvelope
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class YandexGeocodeRetrofitClient(
    baseUrl: HttpUrl?,
    json: Json,
    cachePath: String,
    protocolVersion: Int,
    disableCache: Boolean,
    clientProvider: () -> OkHttpClient,
) : BaseRetrofitClient(baseUrl, json, cachePath, protocolVersion, disableCache, clientProvider) {

    override fun Retrofit.Builder.configureBuild(json: Json) {
        addConverterFactory(
            EnvelopeObjectTypeConverter<YandexGeocodeEnvelope<*>, BaseEnvelopeWithObject<Any>>(
                YandexGeocodeEnvelope::class.java
            )
        )
        addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    }
}