package net.maxsmr.core.network.client.retrofit

import kotlinx.serialization.json.Json
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.handler.CallExceptionHandler
import net.maxsmr.core.network.retrofit.converters.BaseEnvelopeWithObject
import net.maxsmr.core.network.retrofit.converters.EnvelopeObjectTypeConverter
import net.maxsmr.core.network.retrofit.converters.api.YandexGeocodeEnvelope
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class YandexGeocodeRetrofitClient(
    baseUrl: HttpUrl,
    json: Json,
    cache: ResponseBodyCache<*>,
    exceptionHandler: CallExceptionHandler,
    clientProvider: () -> OkHttpClient,
) : RetrofitClient(baseUrl, json,  cache, exceptionHandler, clientProvider) {

    override fun configureBuild(builder: Retrofit.Builder) {
        builder.addConverterFactory(
            EnvelopeObjectTypeConverter<YandexGeocodeEnvelope<*>, BaseEnvelopeWithObject<Any>>(
                YandexGeocodeEnvelope::class.java
            )
        )
        super.configureBuild(builder)
    }
}