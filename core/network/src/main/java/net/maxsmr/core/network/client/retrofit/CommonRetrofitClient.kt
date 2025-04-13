package net.maxsmr.core.network.client.retrofit

import kotlinx.serialization.json.Json
import net.maxsmr.core.network.exceptions.handler.IApiExceptionHandler
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

// TODO remove, неабстрактный RetrofitClient
class CommonRetrofitClient(
    baseUrl: HttpUrl?,
    json: Json,
    cachePath: String,
    protocolVersion: Int,
    disableCache: Boolean,
    exceptionHandler: IApiExceptionHandler,
    clientProvider: () -> OkHttpClient,
) : BaseRetrofitClient(baseUrl, json, cachePath, protocolVersion, disableCache, exceptionHandler, clientProvider)
