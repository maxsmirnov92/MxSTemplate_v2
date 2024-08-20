package net.maxsmr.core.network.retrofit.converters

/**
 * Используется для извлечения объекта из ответа полученного от API
 * Например, для запроса {"errorCode":0,"errorMessage":"","result":{"promotions":[]}} необходимо передать слудеющее
 * значение EnvelopeObjectType("promotions").
 *
 * Если объект отсутствует, то будет выброшено исключение NetworkException(ErrorCode.JSON_PARSE_ERROR)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class EnvelopeObjectType(
    val value: String = "",
)