package net.maxsmr.core.android.coroutines.execute.usecase

import kotlinx.coroutines.CoroutineDispatcher
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.ExecuteResultWrapper

abstract class UseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    /** Executes the use case asynchronously and returns a [ExecuteResult].
     *
     * @return a [ExecuteResult].
     *
     * @param parameters the input parameters to run the use case with
     */
    suspend operator fun invoke(parameters: P): ExecuteResult<R> {
        return ExecuteResultWrapper.wrapResult(coroutineDispatcher) {
            execute(parameters)
        }
    }

    /**
     * Override this to set the code to be executed.
     */
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}