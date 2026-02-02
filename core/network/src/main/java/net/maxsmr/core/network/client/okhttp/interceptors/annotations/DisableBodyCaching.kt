package net.maxsmr.core.network.client.okhttp.interceptors.annotations

/**
 * Использовать в тех методах, где не требуется [net.maxsmr.core.network.client.okhttp.ResponseBodyCache]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisableBodyCaching