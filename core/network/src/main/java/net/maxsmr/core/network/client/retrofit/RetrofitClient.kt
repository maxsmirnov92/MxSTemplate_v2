package net.maxsmr.core.network.client.retrofit

import androidx.annotation.CallSuper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.handler.CallExceptionHandler
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

open class RetrofitClient(
    private val baseUrl: HttpUrl,
    private val json: Json,
    private val cache: ResponseBodyCache<*>,
    private val exceptionHandler: CallExceptionHandler,
    private val clientProvider: () -> OkHttpClient,
) {

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

    fun <T : Any> create(service: Class<T>): T {
        synchronized(this) {
            return instance.create(service)
        }
    }

    @CallSuper
    protected open fun configureBuild(builder: Retrofit.Builder) {
        builder.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        builder.addConverterFactory(ScalarsConverterFactory.create())
    }

    private fun build() = Retrofit.Builder().apply {
        baseUrl(baseUrl)
        addCallAdapterFactory(ExceptionHandlingCallAdapterFactory(
            cache,
        ) {
            exceptionHandler.onException(it)
        })
        callFactory { clientProvider().newCall(it) }
        configureBuild(this)
    }.build()
}