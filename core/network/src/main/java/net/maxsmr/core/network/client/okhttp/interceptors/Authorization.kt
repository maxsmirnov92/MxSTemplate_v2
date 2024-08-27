package net.maxsmr.core.network.client.okhttp.interceptors

/**
 * Данной аннотацией помечаются запросы требующие авторизации. Добавляет заголовок "Authorization"
 *
 * @see AdditionalInfoInterceptor
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Authorization
