package net.maxsmr.core.network.retrofit.interceptors

/**
 * Данной аннотацией помечаются запросы, в которые будут автоматически добавлены сервисные поля
 *
 * @see AdditionalInfoInterceptor
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ServiceFields