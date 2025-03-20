package net.maxsmr.core.utils


inline fun <V : Any?, U : Any?> Result<V>.flatMap(transform: (V) -> Result<U>): Result<U> = try {
    when {
        isSuccess -> transform(getOrThrow())
        else -> Result.failure(exceptionOrNull() ?: RuntimeException())
    }
} catch (ex: Exception) {
    Result.failure(ex)
}