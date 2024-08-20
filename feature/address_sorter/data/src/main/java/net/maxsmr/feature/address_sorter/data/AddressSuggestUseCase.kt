package net.maxsmr.feature.address_sorter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.maxsmr.core.android.coroutines.usecase.FlowUseCase
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.android.coroutines.usecase.asUseCaseResult
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import javax.inject.Inject

class AddressSuggestUseCase @Inject constructor(
    private val repository: AddressRepo,
) : FlowUseCase<Flow<AddressSuggestUseCase.Parameters>, List<AddressSuggest>>(Dispatchers.IO) {

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
                    if (p.query.length <= SUGGEST_THRESHOLD) {
                        repository.updateQuery(p.id, p.query)
                        emit(UseCaseResult.Success(emptyList()))
                    } else {
                        emit(UseCaseResult.Loading)
//                        delay(5000)
                        val result = try {
                            UseCaseResult.Success(repository.suggestWithUpdate(p.id, p.query))
                        } catch (e: Exception) {
                            e.asUseCaseResult()
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