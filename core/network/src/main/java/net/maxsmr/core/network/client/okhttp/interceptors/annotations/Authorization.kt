package net.maxsmr.core.network.client.okhttp.interceptors.annotations

/**
 * Данной аннотацией помечаются запросы требующие авторизации. Добавляет заголовок "Authorization"
 *
 * @see net.maxsmr.core.network.client.okhttp.interceptors.AdditionalInfoInterceptor
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Authorization
