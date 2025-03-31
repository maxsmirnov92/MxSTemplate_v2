package net.maxsmr.core.android.coroutines.execute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
            logger.e("invoke failed", e)
            if (e is CancellationException) {
                // переброс по стандартным правилам
                throw e
            }
            ExecuteResult.Error(e)
        }
    }

    fun <T> wrapFlowResult(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        doExecute: suspend () -> T
    ): Flow<ExecuteResult<T>> {
        return wrapFlow(coroutineContext,
            flow {
                emit(ExecuteResult.Loading)
                try {
                    emit(ExecuteResult.Success(doExecute()))
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }
                    emit(ExecuteResult.Error(e))
                }
            })
    }

    fun <T> Flow<T>.wrapResult(
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Flow<ExecuteResult<T>> {
        return this
            .map<T, ExecuteResult<T>> {
                ExecuteResult.Success(it)
            }
            .onStart { emit(ExecuteResult.Loading) }
            .catch { wrapCause(it) }
            .flowOn(coroutineContext)
    }

    internal fun <T> wrapFlow(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        flow: Flow<ExecuteResult<T>>,
    ): Flow<ExecuteResult<T>> = flow
        .catch {
            logger.e("flow failed", it)
            wrapCause(it)
        }
        .flowOn(coroutineContext)

    private suspend fun <T> FlowCollector<ExecuteResult<T>>.wrapCause(cause: Throwable) {
        if (cause is java.lang.Error) {
            throw cause
        }
        val exception = cause as? Exception ?: Exception(cause)
        emit(ExecuteResult.Error(exception))
    }
}