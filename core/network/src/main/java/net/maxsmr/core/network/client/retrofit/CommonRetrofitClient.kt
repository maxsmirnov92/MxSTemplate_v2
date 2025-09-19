package net.maxsmr.core.network.client.retrofit

import kotlinx.serialization.json.Json
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.handler.CallExceptionHandler
import net.maxsmr.core.network.retrofit.converters.ResponseObjectTypeConverter
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class CommonRetrofitClient(
    baseUrl: HttpUrl,
    json: Json,
    cache: ResponseBodyCache<*>,
    exceptionHandler: CallExceptionHandler,
    clientProvider: () -> OkHttpClient,
) : RetrofitClient(baseUrl, json,  cache, exceptionHandler, clientProvider) {

    override fun configureBuild(builder: Retrofit.Builder) {
        builder.addConverterFactory(ResponseObjectTypeConverter())
        super.configureBuild(builder)
    }
}

