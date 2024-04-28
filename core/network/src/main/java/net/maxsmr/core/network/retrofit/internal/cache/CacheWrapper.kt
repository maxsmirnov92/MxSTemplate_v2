package net.maxsmr.core.network.retrofit.internal.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path
import java.io.Closeable
import java.io.Flushable
import java.lang.reflect.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

//https://jakewharton.com/exceptions-and-proxies-and-coroutines-oh-my/
//https://phase2online.com/2021/05/14/creating-dynamic-proxies-with-kotlin-coroutines/

internal class CacheWrapper(
    json: Json,
    directory: Path,
    fileSystem: FileSystem,
    protocolVersion: Int,
) : Closeable, Flushable {

    internal val cache = FileCache(
        fileSystem = fileSystem,
        directory = directory,
        json = json,
        version = protocolVersion
    )

    fun <T : Any> wrap(interfaceCls: Class<T>, implementation: Any): T =
        Proxy.newProxyInstance(implementation.javaClass.classLoader, arrayOf(interfaceCls)) { _, method, originArgs ->
            process(implementation, method, originArgs)
        } as T

    suspend fun clearCache() {
        cache.clear()
    }

    private fun getParameterLowerBound(index: Int, type: ParameterizedType): Type {
        val types = type.actualTypeArguments
        require(!(index < 0 || index >= types.size)) { "Index " + index + " not in range [0," + types.size + ") for " + type }
        val paramType = types[index]
        return if (paramType is WildcardType) {
            paramType.lowerBounds[0]
        } else paramType
    }

    private fun process(impl: Any, method: Method, originArgs: Array<Any>?): Any {
        val args = originArgs ?: emptyArray()
        val cacheStrategy = method.getAnnotation(Cache::class.java)
        val offlineCacheStrategy = method.getAnnotation(OfflineCache::class.java)

        try {
            val lastArg = args.lastOrNull()
            if ((cacheStrategy == null && offlineCacheStrategy == null) || lastArg == null || lastArg !is Continuation<*>) {
                return method.invoke(impl, *args)
            } else {
                val parameterTypes = method.genericParameterTypes
                val responseType =
                    getParameterLowerBound(0, parameterTypes[parameterTypes.size - 1] as ParameterizedType)
                val returnOfflineStatus = offlineCacheStrategy != null && responseType is ParameterizedType && responseType.rawType == Pair::class.javaObjectType

                val fresh = offlineCacheStrategy?.fresh ?: cacheStrategy.fresh

                val key = key(method, fresh, args)

                @Suppress("UNCHECKED_CAST")
                val originalContinuation = lastArg as Continuation<Any?>
                val wrappedContinuation = WrappedContinuation(
                    cache = cache,
                    key = key,
                    responseType = if (!returnOfflineStatus) responseType else getParameterLowerBound(0, responseType as ParameterizedType),
                    expiresAfterMs = offlineCacheStrategy?.expiresAfterMs() ?: cacheStrategy.expiresAfterMs(),
                    offlineExpiresAfterMs = offlineCacheStrategy?.offlineExpiresAfterMs() ?: 0,
                    offline = offlineCacheStrategy != null,
                    returnOfflineStatus = returnOfflineStatus,
                    originalContinuation = originalContinuation,
                    context = originalContinuation.context
                )

                CoroutineScope(originalContinuation.context).launch(Dispatchers.IO + originalContinuation.context) {
                    val obj = if (fresh) {
                        null
                    } else {
                        cache.get(key, wrappedContinuation.responseType, if (wrappedContinuation.offline) wrappedContinuation.expiresAfterMs else 0)
                    }

                    if (obj == null) {
                        val argumentsWithoutContinuation = args.take(args.size - 1)
                        val newArgs = argumentsWithoutContinuation + wrappedContinuation
                        method.invoke(impl, *newArgs.toTypedArray())
                    } else {
                        if (wrappedContinuation.returnOfflineStatus) {
                            originalContinuation.resumeWith(Result.success(Pair(obj, false)))
                        } else {
                            originalContinuation.resumeWith(Result.success(obj))
                        }
                    }
                }

                return kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
            }
        } catch (e: InvocationTargetException) {
            e.targetException?.let { targetException ->
                throw targetException
            } ?: throw e
        }
    }

    override fun close() {
        cache.close()
    }

    override fun flush() {
        cache.flush()
    }

    private fun key(method: Method, fresh: Boolean, args: Array<out Any>) =
        (with(method) { (declaringClass.canonicalName ?: "") + "." + name.let {
            if (fresh) {
                it.removeSuffix("Fresh")
            } else {
                it
            }
        } } +
                args.filterNot { it is Continuation<*> }.joinToString(
                    prefix = "_",
                    separator = "_"
                ) { it.javaClass.canonicalName + "&" + it.toString() }).encodeUtf8().md5().hex()

    internal class WrappedContinuation(
        val cache: FileCache,
        val key: String,
        val responseType: Type,
        val expiresAfterMs: Long,
        val offlineExpiresAfterMs: Long,
        val offline: Boolean,
        val returnOfflineStatus: Boolean,
        val originalContinuation: Continuation<Any?>,
        override val context: CoroutineContext
    ) : Continuation<Any?> {

        override fun resumeWith(result: Result<Any?>) {
            result.exceptionOrNull()?.let { err ->
                if (offline) {
                    CoroutineScope(originalContinuation.context).launch(Dispatchers.IO + originalContinuation.context) {
                        val obj = cache.get(key, responseType)
                        if (obj != null) {
                            originalContinuation.resumeWith(Result.success(if (returnOfflineStatus) Pair(obj, true) else obj))
                        } else {
                            originalContinuation.resumeWithException(err)
                        }
                    }
                } else {
                    originalContinuation.resumeWithException(err)
                }
            } ?: run {
                CoroutineScope(originalContinuation.context).launch(Dispatchers.IO + originalContinuation.context) {
                    result.getOrNull()?.let { data ->
                        val responseData = if (returnOfflineStatus && data is Pair<*, *>) data.first else data

                        cache.put(
                            FileCache.Entry.Factory(key, Clock.System.now(), if (offline) offlineExpiresAfterMs else expiresAfterMs).create(),
                            responseData as Any,
                            responseType
                        )

                        if (returnOfflineStatus) {
                            originalContinuation.resumeWith(Result.success(Pair(responseData, false)))
                        } else {
                            originalContinuation.resumeWith(result)
                        }

                    } ?: originalContinuation.resumeWith(result)
                }
            }
        }
    }

}