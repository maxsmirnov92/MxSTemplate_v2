package net.maxsmr.core.network.retrofit.internal


/**
 * Используется для методов с проверяемым хэш-кодом, необходим при формировании значения поля hashCode в
 * [AdditionalInfoInterceptor]
 */
internal interface HashCodeMethod {

    fun getHash(): String
}