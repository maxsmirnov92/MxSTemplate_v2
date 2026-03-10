package net.maxsmr.core.network.okhttp

import okhttp3.Request
import retrofit2.Invocation

inline fun <reified T: Annotation> Request.hasAnnotation(): Boolean {
    return tag(Invocation::class.java)?.method()?.isAnnotationPresent(T::class.java) ?: false
}

inline fun <reified T: Annotation> Request.getAnnotation(): T? {
    return tag(Invocation::class.java)?.method()?.getAnnotation(T::class.java)
}