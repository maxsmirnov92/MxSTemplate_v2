package net.maxsmr.core.android.coroutines.execute.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.ExecuteResultWrapper

abstract class FlowResultUseCase<in P, T>(private val coroutineDispatcher: CoroutineDispatcher) {

    operator fun invoke(parameters: P): Flow<ExecuteResult<T>> =
        ExecuteResultWrapper.wrapFlowResult(coroutineDispatcher) {
            execute(parameters)
        }

    protected abstract suspend fun execute(parameters: P): T
}