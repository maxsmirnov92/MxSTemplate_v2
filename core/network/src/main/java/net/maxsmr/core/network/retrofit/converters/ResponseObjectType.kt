package net.maxsmr.core.network.retrofit.converters

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResponseObjectType(
    val value: KClass<out BaseResponse> // будет метод value() в java-аннотации
)