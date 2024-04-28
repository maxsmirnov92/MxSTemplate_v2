package net.maxsmr.core.network.retrofit.interceptors

/**
 * Данной аннотацией помечаются запросы требующие авторизации. Добавляет заголовок "Authorization"
 *
 * @see AdditionalInfoInterceptor
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Authorization
