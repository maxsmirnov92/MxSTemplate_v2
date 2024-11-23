package net.maxsmr.core.network.client.retrofit

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import net.maxsmr.core.network.retrofit.converters.ResponseObjectTypeConverter
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class CommonRetrofitClient(
    baseUrl: HttpUrl?,
    json: Json,
    cachePath: String,
    protocolVersion: Int,
    disableCache: Boolean,
    clientProvider: () -> OkHttpClient,
): BaseRetrofitClient(baseUrl, json, cachePath, protocolVersion, disableCache, clientProvider) {

    override fun Retrofit.Builder.configureBuild(json: Json) {
        addConverterFactory(ResponseObjectTypeConverter())
        addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    }
}

