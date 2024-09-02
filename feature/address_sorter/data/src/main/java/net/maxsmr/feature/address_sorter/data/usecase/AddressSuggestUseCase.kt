package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.FlowUseCase
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.android.coroutines.usecase.asUseCaseResult
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.core.network.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import javax.inject.Inject

class AddressSuggestUseCase @Inject constructor(
    private val addressRepo: AddressRepo,
    private val suggestDataSource: SuggestDataSource,
) : FlowUseCase<Flow<AddressSuggestUseCase.Parameters?>, List<AddressSuggest>>(Dispatchers.IO) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(parameters: Flow<Parameters?>): Flow<UseCaseResult<List<AddressSuggest>>> =
        parameters
            .mapNotNull { it?.let { it.copy(query = it.query.trim()) } }
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
                        addressRepo.updateQuery(p.id, p.query)
                        emit(UseCaseResult.Success(emptyList()))
                    } else {
                        emit(UseCaseResult.Loading)
                        addressRepo.updateQuery(p.id, p.query)
//                        delay(5000)
                        val result = try {
                            val result = suggestDataSource.suggest(p.query, p.lastLocation)
                            if (result.isEmpty()) {
                                throw EmptyResultException(baseApplicationContext, true)
                            } else {
                                UseCaseResult.Success(result)
                            }
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
        val lastLocation: Address.Location?
    )

    companion object {

        private const val SUGGEST_THRESHOLD = 2
        private const val SUGGEST_DELAY = 1300L
    }
}