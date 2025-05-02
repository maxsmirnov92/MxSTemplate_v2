package net.maxsmr.core.utils


inline fun <V : Any?, U : Any?> Result<V>.flatMap(transform: (V) -> Result<U>): Result<U> = try {
    when {
        isSuccess -> transform(getOrThrow())
        else -> Result.failure(exceptionOrNull() ?: RuntimeException())
    }
} catch (ex: Exception) {
    Result.failure(ex)
}

inline fun <V : Any?> Result<V>.flatMapError(transform: (Throwable) -> Result<V>) = try {
    when {
        isSuccess -> Result.success(getOrThrow())
        else -> transform(exceptionOrNull() ?: RuntimeException())
    }
} catch (ex: Exception) {
    Result.failure(ex)
}