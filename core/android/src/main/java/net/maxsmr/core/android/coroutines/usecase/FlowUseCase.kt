package net.maxsmr.core.android.coroutines.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder


abstract class FlowUseCase<in P, T>(private val coroutineDispatcher: CoroutineDispatcher) {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    operator fun invoke(parameters: P): Flow<UseCaseResult<T>> = execute(parameters)
        .catch { e ->
            logger.e(e)
            emit(e.asUseCaseResult())
        }
        .flowOn(coroutineDispatcher)

    protected abstract fun execute(parameters: P): Flow<UseCaseResult<T>>
}
