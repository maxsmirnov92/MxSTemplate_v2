package net.maxsmr.core.network.client.retrofit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.OkHttpException.Companion.orNetworkCause
import net.maxsmr.core.network.retrofit.converters.BaseResponse
import okhttp3.ResponseBody
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.HttpException
import retrofit2.Retrofit
import java.lang.reflect.Type

class ExceptionHandlingCallAdapterFactory(
    private val cache: ResponseBodyCache<*>,
    private val exceptionHandler: (RuntimeException) -> Unit,
) : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *> {
        val delegate = retrofit.nextCallAdapter(this, returnType, annotations)
        return ExceptionHandlingCallAdapter(
            delegate,
            retrofit.responseBodyConverter(
                BaseResponse::class.java, annotations
            )
        )
    }

    private inner class ExceptionHandlingCallAdapter<R>(
        private val delegate: CallAdapter<R, *>,
        private val baseConverter: Converter<ResponseBody, BaseResponse>,
    ) : CallAdapter<R, Any> {

        override fun responseType(): Type = delegate.responseType()

        override fun adapt(call: Call<R>): Any {
            val request = call.request()

            val delegateCall = delegate.adapt(call)
            return if (delegateCall is Call<*>) {

                WrappedCall(
                    delegateCall,
                    onFailure = { e ->
                        val cause = e.orNetworkCause()
                        var resultThrowable = cause
                        if (e is HttpException) {
                            // исходное исключение от http, но надо проверить json-тело
                            // для возможной подмены на ApiException
                            try {
                                // на этом этапе ответ уже был прочитан,
                                // тело имеет тип "NoContentBody",
                                // поэтому извлекаем из кэша склонированное
                                cache.get(request)?.let { body ->
                                    baseConverter.convert(body)
                                }
                            } catch (e: Exception) {
                                // прочие возникшие здесь исключения игнорируются
                                if (e is ApiException) {
                                    resultThrowable = e
                                }
                            }
                        }
                        resultThrowable.let {
                            exceptionHandler(it)
                        }
                        cache.removeWithClose(request)
                        resultThrowable
                    },
                    onSuccess = {
                        cache.removeWithClose(request)
                    }
                )

            } else {
                delegateCall
            }
        }
    }

    private class WrappedCall<R>(
        private val delegate: Call<R>,
        private val onFailure: (Throwable) -> Throwable,
        private val onSuccess: () -> Unit,
    ) : Call<R> {

        override fun enqueue(callback: Callback<R>) {
            delegate.enqueue(object : Callback<R> {

                override fun onResponse(call: Call<R>, response: retrofit2.Response<R>) {
                    if (response.isSuccessful) {
                        try {
                            callback.onResponse(call, response)
                            onSuccess()
                        } catch (e: Exception) {
                            onFailure(call, e)
                        }
                    } else {
                        onFailure(call, HttpException(response))
                    }
                }

                override fun onFailure(call: Call<R>, t: Throwable) {
                    val resultThrowable = onFailure(t)
                    // вернуть исходное или другое исключение
                    callback.onFailure(call, resultThrowable)
                }
            })
        }

        override fun execute(): retrofit2.Response<R> {
            val response = delegate.execute()
            return if (response.isSuccessful) {
                response
            } else {
                throw HttpException(response)
            }
        }

        override fun clone(): Call<R> = WrappedCall(delegate.clone(), onFailure, onSuccess)

        override fun cancel() = delegate.cancel()

        override fun request() = delegate.request()

        override fun timeout(): Timeout = delegate.timeout()

        override fun isExecuted() = delegate.isExecuted

        override fun isCanceled() = delegate.isCanceled
    }

}