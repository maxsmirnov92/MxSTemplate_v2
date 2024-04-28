package net.maxsmr.core.network.retrofit.interceptors

/**
 * Данной аннотацией помечаются запросы требующие авторизации. Добавляет в тело запроса параметр "sessionId"
 *
 * @see AdditionalInfoInterceptor
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Session
