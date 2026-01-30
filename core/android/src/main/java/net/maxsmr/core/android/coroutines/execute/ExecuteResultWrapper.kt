package net.maxsmr.core.android.coroutines.execute

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

object ExecuteResultWrapper {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("ExecuteResultWrapper")

    suspend fun <T> wrapResult(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        doExecute: suspend () -> T,
    ): ExecuteResult<T> {
        return try {
            withContext(coroutineContext) {
                ExecuteResult.Success(doExecute())
            }
        } catch (e: Exception) {
            logger.e("execute failed", e)
            wrapCause(e)
        }
    }

    fun <T> wrapFlowResult(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        doExecute: suspend () -> T,
    ): Flow<ExecuteResult<T>> {
        return flow {
            emit(ExecuteResult.Loading())
            try {
                emit(ExecuteResult.Success(doExecute()))
            } catch (e: Exception) {
                emit(wrapCause(e))
            }
        }.flowOn(coroutineContext)
    }

    fun <T> wrapFlowProgressResult(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        doExecute: suspend (CoroutineContext, ProgressListener) -> T,
    ): Flow<ExecuteResult<T>> {
        return callbackFlow {
            trySend(ExecuteResult.Loading())

            try {
                val result = doExecute(currentCoroutineContext(), ProgressListener {
                    trySend(ExecuteResult.Loading(it))
                })
                trySend(ExecuteResult.Success(result))
            } catch (e: Exception) {
                Log.e("WrapResultExt", "wrap flow progress result failed", e)
                trySend(wrapCause(e))
            } finally {
                close()
            }
        }.flowOn(coroutineContext)
    }

    internal fun <T> wrapFlow(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        flow: Flow<ExecuteResult<T>>,
    ): Flow<ExecuteResult<T>> = flow
        .catch {
            Log.e("WrapResultExt", "wrap flow failed", it)
            wrapCause(it)
        }
        .flowOn(coroutineContext)

    private fun wrapCause(cause: Throwable): ExecuteResult.Error {
        if (cause is java.lang.Error) {
            throw cause
        }
        if (cause is CancellationException) {
            // переброс по стандартным правилам
            throw cause
        }
        val exception = cause as? Exception ?: Exception(cause)
        return ExecuteResult.Error(exception)
    }

    fun interface ProgressListener {

        fun onProgress(progress: Float?)
    }
}