package net.maxsmr.core.network.client.okhttp

import okhttp3.Request
import okhttp3.ResponseBody
import java.util.concurrent.ConcurrentHashMap

/**
 * Кэш со всеми выполняемыми OkHttp-вызовами
 * с целью переиспользования тела ответа
 */
class ResponseBodyCache<Key>(private val keyProvider: Request.() -> Key) {

    private val cache = ConcurrentHashMap<Key, ResponseBody>()

    fun store(request: Request, body: ResponseBody) {
        cache[request.keyProvider()] = body
    }

    fun get(request: Request): ResponseBody? = cache[request.keyProvider()]

    fun remove(request: Request): ResponseBody? = cache.remove(request.keyProvider())

    fun removeWithClose(request: Request) {
        remove(request)?.close()
    }

    fun clear() {
        cache.clear()
    }
}