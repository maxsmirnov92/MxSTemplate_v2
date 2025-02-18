package net.maxsmr.core.network.client.retrofit

import kotlinx.serialization.json.Json
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.handler.ICallExceptionHandler
import net.maxsmr.core.network.retrofit.converters.ResponseObjectTypeConverter
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class CommonRetrofitClient(
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
        builder.addConverterFactory(ResponseObjectTypeConverter())
        super.configureBuild(builder, json)
    }
}

