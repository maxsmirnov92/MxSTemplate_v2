package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.exceptions.OkHttpException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * [Interceptor] для переброса исключений из других в IOException.
 * Регистрировать первым в цепочке!
 */
class ExceptionHandlingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: IOException) {
            // брошенный IOException в Interceptor будет передан в неизменном виде
            throw e
        } catch (e: RuntimeException) {
            // RuntimeException будет обёрнут в IOException с сообщением "canceled due to..."
            // и приложение в любом случае упадёт, что не вариант - бросаем кастомный IOException для обработки позднее
            throw OkHttpException(e)
        }
    }
}