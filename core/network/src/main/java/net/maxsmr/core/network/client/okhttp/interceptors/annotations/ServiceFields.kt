package net.maxsmr.core.network.client.okhttp.interceptors.annotations

/**
 * Данной аннотацией помечаются запросы, в которые будут автоматически добавлены сервисные поля
 *
 * @see net.maxsmr.core.network.client.okhttp.interceptors.AdditionalInfoInterceptor
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ServiceFields