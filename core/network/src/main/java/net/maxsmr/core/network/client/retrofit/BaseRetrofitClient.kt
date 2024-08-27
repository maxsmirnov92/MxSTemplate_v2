package net.maxsmr.core.network.client.retrofit

import kotlinx.serialization.json.Json
import net.maxsmr.core.network.retrofit.internal.cache.CacheWrapper
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okio.FileSystem
import okio.Path.Companion.toPath
import retrofit2.Retrofit

abstract class BaseRetrofitClient(
    protected val baseUrl: HttpUrl?,
    protected val client: OkHttpClient,
    protected val json: Json,
    protected val cachePath: String,
    protected val protocolVersion: Int,
    protected val disableCache: Boolean,
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

    protected abstract fun Retrofit.Builder.configureBuild(json: Json)

    private fun build() = Retrofit.Builder().apply {
        baseUrl?.let { baseUrl(it) }
        client(client)
        configureBuild(json)
    }.build()
}