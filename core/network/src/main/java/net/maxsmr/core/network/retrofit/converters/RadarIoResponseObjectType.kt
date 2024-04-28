package net.maxsmr.core.network.retrofit.converters

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class RadarIoResponseObjectType(
    val value: KClass<out BaseRadarIoResponse>
)