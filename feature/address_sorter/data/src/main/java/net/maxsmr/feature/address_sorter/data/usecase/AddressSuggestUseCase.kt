package net.maxsmr.feature.address_sorter.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.maxsmr.core.android.coroutines.execute.usecase.base.BaseInputSearchUseCase
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AddressSuggestUseCase @Inject constructor(
    private val addressRepo: AddressRepo,
    private val suggestDataSource: SuggestDataSource,
    @ApplicationContext private val context: Context,
) : BaseInputSearchUseCase<AddressSuggestUseCase.Parameters, List<AddressSuggest>>() {

    override val inputDelay: Duration = 1300.milliseconds

    override fun getInputThreshold(input: String): Int = 2

    override suspend fun doAction(params: Parameters): List<AddressSuggest> {
        addressRepo.updateQuery(params.id, params.input)
        val result = suggestDataSource.suggest(params.input, params.lastLocation)
        if (result.isEmpty()) {
            throw EmptyResultException(context, true)
        }
        return result
    }

    override suspend fun doActionNoSearch(params: Parameters): List<AddressSuggest> {
        addressRepo.updateQuery(params.id, params.input)
        return listOf()
    }

    override fun Parameters.copy(input: String): Parameters = copy(input = input)

    data class Parameters(
        override val input: String,
        val id: Long,
        val lastLocation: Address.Location?,
    ) : SearchParameters
}