package net.maxsmr.core.network.client.retrofit

import kotlinx.serialization.json.Json
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.handler.ICallExceptionHandler
import net.maxsmr.core.network.retrofit.converters.BaseEnvelopeWithObject
import net.maxsmr.core.network.retrofit.converters.EnvelopeObjectTypeConverter
import net.maxsmr.core.network.retrofit.converters.api.VkResponseEnvelope
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class VkRetrofitClient(
    baseUrl: HttpUrl?,
    json: Json,
    cachePath: String,
    protocolVersion: Int,
    disableCache: Boolean,
    cache: ResponseBodyCache<*>,
    exceptionHandler: ICallExceptionHandler,
    clientProvider: () -> OkHttpClient,
) : RetrofitClient(baseUrl, json, cachePath, protocolVersion, disableCache, cache, exceptionHandler, clientProvider) {

    override fun configureBuild(builder: Retrofit.Builder) {
        builder.addConverterFactory(
            EnvelopeObjectTypeConverter<VkResponseEnvelope<*>, BaseEnvelopeWithObject<Any>>(
                VkResponseEnvelope::class.java
            )
        )
        super.configureBuild(builder)
    }
}