package net.maxsmr.core.network.client.retrofit

import kotlinx.serialization.json.Json
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.handler.ICallExceptionHandler
import net.maxsmr.core.network.retrofit.converters.BaseEnvelopeWithObject
import net.maxsmr.core.network.retrofit.converters.EnvelopeObjectTypeConverter
import net.maxsmr.core.network.retrofit.converters.api.VmOsCloudEnvelope
import net.maxsmr.core.network.retrofit.converters.api.YandexGeocodeEnvelope
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class VmOsCloudRetrofitClient(
    baseUrl: HttpUrl?,
    json: Json,
    cachePath: String,
    protocolVersion: Int,
    disableCache: Boolean,
    cache: ResponseBodyCache<*>,
    exceptionHandler: ICallExceptionHandler,
    clientProvider: () -> OkHttpClient,
) : BaseRetrofitClient(baseUrl, json, cachePath, protocolVersion, disableCache, cache, exceptionHandler, clientProvider) {

    override fun configureBuild(builder: Retrofit.Builder, json: Json) {
        builder.addConverterFactory(
            EnvelopeObjectTypeConverter<VmOsCloudEnvelope<*>, BaseEnvelopeWithObject<Any>>(
                VmOsCloudEnvelope::class.java
            )
        )
        super.configureBuild(builder, json)
    }
}