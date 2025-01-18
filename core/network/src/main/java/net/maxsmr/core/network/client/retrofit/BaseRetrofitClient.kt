package net.maxsmr.core.network.client.retrofit

import androidx.annotation.CallSuper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import net.maxsmr.core.network.exceptions.handler.IApiExceptionHandler
import net.maxsmr.core.network.retrofit.internal.cache.CacheWrapper
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okio.FileSystem
import okio.Path.Companion.toPath
import retrofit2.Retrofit
import java.lang.StringBuilder

abstract class BaseRetrofitClient(
    private val baseUrl: HttpUrl?,
    private val json: Json,
    private val cachePath: String,
    private val protocolVersion: Int,
    private val disableCache: Boolean,
    private val exceptionHandler: IApiExceptionHandler,
    private val clientProvider: () -> OkHttpClient,
) {

    @Volatile
    lateinit var instance: Retrofit
        private set

    private var cacheWrapper: CacheWrapper? = null

    fun init() {
        synchronized(this) {
            if (!::instance.isInitialized) {
                if (!disableCache) {
                    cacheWrapper =
                        CacheWrapper(json, cachePath.toPath(), FileSystem.SYSTEM, protocolVersion)
                }

                instance = build()
            }
        }
    }

    suspend fun clearCache() {
        cacheWrapper?.clearCache()
    }

    fun <T : Any> create(service: Class<T>): T {
        synchronized(this) {
            return cacheWrapper?.wrap(service, instance.create(service)) ?: instance.create(service)
        }
    }

    @CallSuper
    protected open fun configureBuild(builder: Retrofit.Builder, json: Json) {
        builder.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    }

    private fun build() = Retrofit.Builder().apply {
        baseUrl?.let { baseUrl(it) }
        addCallAdapterFactory(ExceptionHandlingCallAdapterFactory(exceptionHandler))
        callFactory { clientProvider().newCall(it) }
        configureBuild(this, json)
    }.build()
}