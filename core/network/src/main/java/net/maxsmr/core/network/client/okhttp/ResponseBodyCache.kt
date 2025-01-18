package net.maxsmr.core.network.client.okhttp

import okhttp3.Request
import okhttp3.ResponseBody

internal object ResponseBodyCache {

    private val cache = mutableMapOf<Request, ResponseBody>()

    fun store(request: Request, body: ResponseBody) {
        cache[request] = body
    }

    fun get(request: Request): ResponseBody? = cache[request]

    fun remove(request: Request): ResponseBody? = cache.remove(request)

    fun clear() {
        cache.clear()
    }
}