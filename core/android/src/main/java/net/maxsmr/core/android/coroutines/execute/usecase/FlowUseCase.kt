package net.maxsmr.core.android.coroutines.execute.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.ExecuteResultWrapper

abstract class FlowUseCase<in P, T>(private val coroutineDispatcher: CoroutineDispatcher) {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    operator fun invoke(parameters: P): Flow<ExecuteResult<T>> =
        ExecuteResultWrapper.wrapFlow(coroutineDispatcher, execute(parameters))

    protected abstract fun execute(parameters: P): Flow<ExecuteResult<T>>
}
