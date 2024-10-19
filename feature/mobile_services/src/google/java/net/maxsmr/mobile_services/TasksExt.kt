package net.maxsmr.mobile_services

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resumeWithException

suspend fun <T, R> com.google.android.gms.tasks.Task<T>.asCoroutine(mapResultFunc: (T) -> R): R {
    return suspendCancellableCoroutine { continuation ->
//            continuation.invokeOnCancellation {
//                if (task is CancellableTask) {
//                    task.cancel()
//                }
//            }
        this.addOnSuccessListener {
            continuation.resume(mapResultFunc(it))
        }
        this.addOnFailureListener {
            continuation.resumeWithException(it)
        }
        this.addOnCanceledListener {
            continuation.resumeWithException(CancellationException())
        }
    }
}

suspend fun <T, R> ru.rustore.sdk.core.tasks.Task<T>.asCoroutine(mapResultFunc: (T) -> R): R {
    return suspendCancellableCoroutine { continuation ->
        this.addOnSuccessListener {
            continuation.resume(mapResultFunc(it))
        }
        this.addOnFailureListener {
            continuation.resumeWithException(it)
        }
    }
}