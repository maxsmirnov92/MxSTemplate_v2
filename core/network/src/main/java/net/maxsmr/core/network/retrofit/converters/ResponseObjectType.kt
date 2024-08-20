package net.maxsmr.core.network.retrofit.converters

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ResponseObjectType(
    val value: KClass<out BaseResponse>
)