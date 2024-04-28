package net.maxsmr.core.network.retrofit.internal.cache


/**
 * Данной анатацией помечаются запросы, результат выполнения которых должен быть помещен в кэш
 *
 * @param fresh - получить новые данные игнорируя содержимое кэша, результат сохраняется в кэш
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cache(val expiresAfter: Long, val unit: TimeUnit, val fresh: Boolean = false)


/**
 * Данной анатацией помечаются запросы, результат выполнения которых должен быть помещен в кэш и может быть
 * использован в случае если не удалось получить корректный ответ от сервера
 *
 * @param fresh - получить новые данные игнорируя содержимое кэша, результат сохраняется в кэш
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OfflineCache(
    val expiresAfter: Long,
    val unit: TimeUnit,
    val offlineExpiresAfter: Long,
    val offlineUnit: TimeUnit,
    val fresh: Boolean = false,
)

fun Cache.expiresAfterMs(): Long {
    return when (this.unit) {
        TimeUnit.SECONDS -> this.expiresAfter * 1000
        TimeUnit.MINUTES -> this.expiresAfter * 60000
        TimeUnit.HOURS -> this.expiresAfter * 3600000
        TimeUnit.DAYS -> this.expiresAfter * 86400000
    }
}

fun OfflineCache.expiresAfterMs(): Long {
    return when (this.unit) {
        TimeUnit.SECONDS -> this.expiresAfter * 1000
        TimeUnit.MINUTES -> this.expiresAfter * 60000
        TimeUnit.HOURS -> this.expiresAfter * 3600000
        TimeUnit.DAYS -> this.expiresAfter * 86400000
    }
}

fun OfflineCache.offlineExpiresAfterMs(): Long {
    return when (this.offlineUnit) {
        TimeUnit.SECONDS -> this.offlineExpiresAfter * 1000
        TimeUnit.MINUTES -> this.offlineExpiresAfter * 60000
        TimeUnit.HOURS -> this.offlineExpiresAfter * 3600000
        TimeUnit.DAYS -> this.offlineExpiresAfter * 86400000
    }
}

enum class TimeUnit {
    SECONDS,
    MINUTES,
    HOURS,
    DAYS
}