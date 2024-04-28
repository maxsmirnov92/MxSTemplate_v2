package net.maxsmr.core.network.retrofit.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import net.maxsmr.core.network.retrofit.converters.RadarIoEnvelopingConverter
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class RadarIoRetrofitClient(
    baseUrl: HttpUrl?,
    client: OkHttpClient,
    json: Json,
    cachePath: String,
    protocolVersion: Int,
    disableCache: Boolean,
): BaseRetrofitClient(baseUrl, client, json, cachePath, protocolVersion, disableCache) {

    override fun Retrofit.Builder.configureBuild(json: Json) {
        addConverterFactory(RadarIoEnvelopingConverter(null))
        addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    }
}

