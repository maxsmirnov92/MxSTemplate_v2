package net.maxsmr.feature.address_sorter

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.maxsmr.core.android.coroutines.usecase.FlowUseCase
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.feature.address_sorter.repository.AddressRepo
import javax.inject.Inject

class AddressSuggestUseCase @Inject constructor(
    private val repository: AddressRepo,
    @Dispatcher(AppDispatchers.IO)
    ioDispatcher: CoroutineDispatcher,
) : FlowUseCase<Flow<AddressSuggestUseCase.Parameters>, List<AddressSuggest>>(ioDispatcher) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(parameters: Flow<Parameters>): Flow<UseCaseResult<List<AddressSuggest>>> =
        parameters
            .map { it.copy(query = it.query.trim()) }
            .debounce {
                if (it.query.length < SUGGEST_THRESHOLD) {
                    0
                } else {
                    SUGGEST_DELAY
                }
            }
            .distinctUntilChanged()
            .flatMapLatest { p ->
                flow {
                    if (p.query.length < SUGGEST_THRESHOLD) {
                        emit(UseCaseResult.Success(emptyList()))
                    } else {
                        emit(UseCaseResult.Loading)
                        val result = try {
                            UseCaseResult.Success(repository.suggestWithRefresh(p.id, p.query))
                        } catch (e: Exception) {
                            UseCaseResult.Error(e)
                        }
                        emit(result)
                    }
                }
            }

    data class Parameters(
        val id: Long,
        val query: String,
    )

    companion object {

        private const val SUGGEST_THRESHOLD = 2
        private const val SUGGEST_DELAY = 500L
    }
}