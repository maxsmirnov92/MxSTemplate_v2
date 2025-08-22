package net.maxsmr.core.network.client.retrofit

import androidx.annotation.CallSuper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.handler.CallExceptionHandler
import net.maxsmr.core.network.retrofit.internal.cache.CacheWrapper
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okio.FileSystem
import okio.Path.Companion.toPath
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

open class RetrofitClient(
    private val baseUrl: HttpUrl?,
    private val json: Json,
    private val cachePath: String,
    private val protocolVersion: Int,
    private val disableCache: Boolean,
    private val cache: ResponseBodyCache<*>,
    private val exceptionHandler: CallExceptionHandler,
    private val clientProvider: () -> OkHttpClient,
) {

    private val cacheWrapper: CacheWrapper? by lazy {
        if (!disableCache) {
            CacheWrapper(json, cachePath.toPath(), FileSystem.SYSTEM, protocolVersion)
        } else {
            null
        }
    }

    @Volatile
    lateinit var instance: Retrofit
        private set

    fun init() {
        synchronized(this) {
            if (!::instance.isInitialized) {
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
    protected open fun configureBuild(builder: Retrofit.Builder) {
        builder.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        builder.addConverterFactory(ScalarsConverterFactory.create())
    }

    private fun build() = Retrofit.Builder().apply {
        baseUrl?.let { baseUrl(it) }
        addCallAdapterFactory(ExceptionHandlingCallAdapterFactory(
            cache,
        ) {
            exceptionHandler.onException(it)
        })
        callFactory { clientProvider().newCall(it) }
        configureBuild(this)
    }.build()
}