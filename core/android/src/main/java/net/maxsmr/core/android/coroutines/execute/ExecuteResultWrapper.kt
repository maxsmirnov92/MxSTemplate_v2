package net.maxsmr.core.android.coroutines.execute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import kotlin.coroutines.CoroutineContext

object ExecuteResultWrapper {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("ExecuteResultWrapper")

    suspend fun <T> wrapResult(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        doExecute: suspend () -> T,
    ): ExecuteResult<T> {
        return try {
            // Moving all use case's executions to the injected dispatcher
            // In production code, this is usually the Default dispatcher (background thread)
            // In tests, this becomes a TestCoroutineDispatcher
            withContext(coroutineContext) {
                ExecuteResult.Success(doExecute())
            }
        } catch (e: Throwable) {
            logger.e("wrapResult failed", e)
            e.asExecuteResult()
        }
    }

    fun <T> wrapFlowResult(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        doExecute: suspend () -> T,
    ): Flow<ExecuteResult<T>> {
        return wrapFlow(coroutineContext,
            flow {
                emit(ExecuteResult.Loading)
                try {
                    emit(ExecuteResult.Success(doExecute()))
                } catch (e: Exception) {
                    emit(ExecuteResult.Error(e))
                }
            })
    }

    internal fun <T> wrapFlow(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        flow: Flow<ExecuteResult<T>>,
    ): Flow<ExecuteResult<T>> = flow
        .catch { e ->
            logger.e("wrapFlow catch exception", e)
            emit(e.asExecuteResult())
        }
        .flowOn(coroutineContext)

}