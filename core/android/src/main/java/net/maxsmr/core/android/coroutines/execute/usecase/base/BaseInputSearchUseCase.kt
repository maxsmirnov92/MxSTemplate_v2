package net.maxsmr.core.android.coroutines.execute.usecase.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.ExecuteResultWrapper
import net.maxsmr.core.android.coroutines.execute.usecase.FlowUseCase
import net.maxsmr.core.android.coroutines.execute.usecase.base.BaseInputSearchUseCase.SearchParameters
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * [FlowUseCase] для выполнения действия поиска при появлении новых строк в исходном [Flow]
 */
abstract class BaseInputSearchUseCase<P : SearchParameters, T> :
        FlowUseCase<Flow<P>, BaseInputSearchUseCase.SearchResult<T>>(Dispatchers.IO) {

    /**
     * Задержка после ввода перед вызовом действия, мс
     */
    protected abstract val inputDelay: Duration

    protected open val withDistinctUntilChanged: Boolean = true

    /**
     * Кол-во символов, начиная с которого нужно производить поиск при текущей строке
     */
    protected abstract fun getInputThreshold(input: String): Int

    protected abstract suspend fun doAction(params: P): T

    protected abstract suspend fun doActionNoSearch(params: P): T

    protected abstract fun P.copy(input: String): P

    protected open fun filterInput(input: String): String {
        return input.trim()
    }

    protected open fun shouldSearch(params: P): Boolean {
        return with(params.input) { isNotEmpty() && length >= getInputThreshold(this) }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    final override fun execute(parameters: Flow<P>): Flow<ExecuteResult<SearchResult<T>>> =
        parameters
            .mapNotNull { it.copy(filterInput(it.input)) }
            .debounce {
                val delay = inputDelay
                if (shouldSearch(it) && delay.isPositive()) {
                    delay
                } else {
                    0.milliseconds
                }
            }
            .let {
                if (withDistinctUntilChanged) {
                    it.distinctUntilChanged()
                } else {
                    it
                }
            }
            .flatMapLatest { p ->
                if (shouldSearch(p)) {
                    ExecuteResultWrapper.wrapFlowResult {
                        SearchResult(doAction(p), true)
                    }
                } else {
                    ExecuteResultWrapper.wrapFlowResult {
                        SearchResult(doActionNoSearch(p), false)
                    }
                }
            }

    interface SearchParameters {

        val input: String
    }

    data class SearchResult<T>(
        val data: T,
        val wasSearched: Boolean,
    )
}