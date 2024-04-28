package net.maxsmr.core.android.coroutines.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder


abstract class UseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    /** Executes the use case asynchronously and returns a [UseCaseResult].
     *
     * @return a [UseCaseResult].
     *
     * @param parameters the input parameters to run the use case with
     */
    suspend operator fun invoke(parameters: P): UseCaseResult<R> {
        runBlocking {
            coroutineContext[Job]
        }
        return try {
            // Moving all use case's executions to the injected dispatcher
            // In production code, this is usually the Default dispatcher (background thread)
            // In tests, this becomes a TestCoroutineDispatcher
            withContext(coroutineDispatcher) {
                execute(parameters).let {
                    UseCaseResult.Success(it)
                }
            }
        } catch (e: Throwable) {
            logger.e(e)
            e.asUseCaseResult()
        }
    }

    /**
     * Override this to set the code to be executed.
     */
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}
